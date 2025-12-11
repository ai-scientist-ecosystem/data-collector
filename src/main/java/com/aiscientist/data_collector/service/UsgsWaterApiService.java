package com.aiscientist.data_collector.service;

import com.aiscientist.data_collector.dto.UsgsWaterResponse;
import com.aiscientist.data_collector.dto.WaterLevelEvent;
import com.aiscientist.data_collector.model.WaterLevelMetric;
import com.aiscientist.data_collector.repository.WaterLevelMetricRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Service to collect river/stream water level data from USGS Water Services API
 * API Documentation: https://waterservices.usgs.gov/rest/IV-Service.html
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsgsWaterApiService {

    private final WebClient.Builder webClientBuilder;
    private final WaterLevelMetricRepository waterLevelRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.usgs.water.base-url:https://waterservices.usgs.gov/nwis/iv}")
    private String baseUrl;

    // Major river monitoring sites across US
    // Format: siteCode
    private static final List<String> MONITORING_SITES = Arrays.asList(
        "01646500",  // Potomac River at Little Falls, DC
        "02035000",  // James River at Richmond, VA
        "02089500",  // Neuse River at Kinston, NC
        "02169500",  // Congaree River at Columbia, SC
        "02228000",  // Altamaha River at Doctortown, GA
        "07374000",  // Mississippi River at Baton Rouge, LA
        "08074000",  // Buffalo Bayou at Houston, TX
        "09380000",  // Colorado River at Lee's Ferry, AZ
        "11447650",  // Sacramento River at Freeport, CA
        "12113390",  // Cedar River at Renton, WA
        "01463500",  // Delaware River at Trenton, NJ
        "01589000",  // Jones Falls at Sorrento, Baltimore, MD
        "03234500"   // Scioto River at Columbus, OH
    );

    /**
     * Fetch water level and discharge for a specific USGS site
     */
    @CircuitBreaker(name = "usgs-water-api", fallbackMethod = "fetchWaterDataFallback")
    @Retry(name = "usgs-water-api")
    @Cacheable(value = "usgs-water", key = "#siteCode", unless = "#result == null")
    public Mono<WaterLevelMetric> fetchWaterData(String siteCode) {
        log.info("Fetching water data for USGS site: {}", siteCode);

        WebClient webClient = webClientBuilder
            .baseUrl(baseUrl)
            .build();

        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .queryParam("format", "json")
                .queryParam("sites", siteCode)
                .queryParam("parameterCd", "00065,00060") // Gage height, Discharge
                .queryParam("siteStatus", "active")
                .build())
            .retrieve()
            .bodyToMono(UsgsWaterResponse.class)
            .map(response -> convertToMetric(response, siteCode))
            .doOnSuccess(metric -> {
                if (metric != null) {
                    waterLevelRepository.save(metric);
                    log.info("Saved water data for site {}: {} ft", 
                        siteCode, metric.getGageHeightFeet());
                }
            })
            .doOnError(error -> log.error("Error fetching water data for site {}", siteCode, error))
            .onErrorResume(error -> {
                log.error("Failed to fetch water data for site {}: {}", siteCode, error.getMessage());
                return Mono.empty();
            });
    }

    /**
     * Fetch water data for all monitoring sites
     */
    public Flux<WaterLevelMetric> fetchAllMonitoringSites() {
        log.info("Fetching water data for {} monitoring sites", MONITORING_SITES.size());
        
        return Flux.fromIterable(MONITORING_SITES)
            .flatMap(siteCode -> fetchWaterData(siteCode)
                .delayElement(java.time.Duration.ofMillis(150))) // Rate limiting
            .doOnComplete(() -> log.info("Completed fetching all monitoring sites"));
    }

    /**
     * Convert USGS API response to WaterLevelMetric entity
     */
    private WaterLevelMetric convertToMetric(UsgsWaterResponse response, String siteCode) {
        try {
            if (response == null || response.getValue() == null || 
                response.getValue().getTimeSeries() == null || 
                response.getValue().getTimeSeries().length == 0) {
                log.warn("No water data found for site {}", siteCode);
                return null;
            }

            UsgsWaterResponse.TimeSeries[] timeSeries = response.getValue().getTimeSeries();
            
            WaterLevelMetric.WaterLevelMetricBuilder builder = WaterLevelMetric.builder()
                .stationId(siteCode)
                .source("usgs_water")
                .locationType(determineLocationType());

            // Process each time series (gage height, discharge, etc.)
            for (UsgsWaterResponse.TimeSeries ts : timeSeries) {
                if (ts.getValues() == null || ts.getValues().length == 0) continue;
                
                UsgsWaterResponse.DataValue[] values = ts.getValues()[0].getValue();
                if (values == null || values.length == 0) continue;

                UsgsWaterResponse.DataValue latestValue = values[0];
                UsgsWaterResponse.SourceInfo sourceInfo = ts.getSourceInfo();
                String variableCode = ts.getVariable().getVariableCode();

                // Set common fields from first time series
                if (sourceInfo != null) {
                    builder.stationName(sourceInfo.getSiteName());
                    if (sourceInfo.getGeoLocation() != null && 
                        sourceInfo.getGeoLocation().getGeogLocation() != null) {
                        builder.latitude(sourceInfo.getGeoLocation().getGeogLocation().getLatitude())
                               .longitude(sourceInfo.getGeoLocation().getGeogLocation().getLongitude());
                    }
                }

                Instant timestamp = ZonedDateTime.parse(latestValue.getDateTime()).toInstant();
                builder.timestamp(timestamp);

                String qualifiers = latestValue.getQualifiers() != null && latestValue.getQualifiers().length > 0
                    ? String.join(",", latestValue.getQualifiers()) : null;
                builder.qualityCode(qualifiers);

                // Parse variable-specific data
                double value = Double.parseDouble(latestValue.getValue());
                
                if (variableCode.contains("00065")) {
                    // Gage height in feet
                    builder.gageHeightFeet(value)
                           .waterLevelFeet(value)
                           .waterLevelMeters(value * 0.3048); // Convert feet to meters
                } else if (variableCode.contains("00060")) {
                    // Discharge in cubic feet per second
                    builder.dischargeCfs(value);
                }
            }

            builder.rawData(objectMapper.writeValueAsString(response))
                   .processedAt(Instant.now());

            return builder.build();

        } catch (Exception e) {
            log.error("Error converting USGS response to metric for site {}", siteCode, e);
            return null;
        }
    }

    /**
     * Create Kafka event from water level metric
     */
    public WaterLevelEvent createEvent(WaterLevelMetric metric) {
        return WaterLevelEvent.builder()
            .stationId(metric.getStationId())
            .stationName(metric.getStationName())
            .source(metric.getSource())
            .locationType(metric.getLocationType())
            .latitude(metric.getLatitude())
            .longitude(metric.getLongitude())
            .timestamp(metric.getTimestamp())
            .waterLevelMeters(metric.getWaterLevelMeters())
            .waterLevelFeet(metric.getWaterLevelFeet())
            .dischargeCfs(metric.getDischargeCfs())
            .gageHeightFeet(metric.getGageHeightFeet())
            .floodStageFeet(metric.getFloodStageFeet())
            .floodSeverity(metric.getFloodSeverity())
            .isFlooding(metric.isFlooding())
            .qualityCode(metric.getQualityCode())
            .build();
    }

    /**
     * Determine location type - USGS sites are primarily rivers/streams
     */
    private String determineLocationType() {
        return "river";
    }

    /**
     * Fallback method when circuit breaker opens
     */
    private Mono<WaterLevelMetric> fetchWaterDataFallback(String siteCode, Exception e) {
        log.warn("Circuit breaker activated for USGS Water API (site {}), returning cached data", siteCode, e);
        
        // Try to return latest cached data from database
        return Mono.fromCallable(() -> 
            waterLevelRepository.findFirstByStationIdOrderByTimestampDesc(siteCode).orElse(null));
    }

    /**
     * Get list of all monitored site codes
     */
    public List<String> getMonitoringSites() {
        return MONITORING_SITES;
    }
}
