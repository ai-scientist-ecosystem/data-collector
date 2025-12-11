package com.aiscientist.data_collector.service;

import com.aiscientist.data_collector.dto.NoaaTidesResponse;
import com.aiscientist.data_collector.dto.WaterLevelEvent;
import com.aiscientist.data_collector.exception.ExternalApiException;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Service to collect water level data from NOAA CO-OPS Tides and Currents API
 * API Documentation: https://api.tidesandcurrents.noaa.gov/api/prod/
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NoaaTidesApiService {

    private final WebClient.Builder webClientBuilder;
    private final WaterLevelMetricRepository waterLevelRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.noaa.tides.base-url:https://api.tidesandcurrents.noaa.gov/api/prod}")
    private String baseUrl;

    @Value("${app.noaa.tides.application:ai-scientist-ecosystem}")
    private String application;

    // Major US coastal stations for monitoring
    private static final List<String> MONITORING_STATIONS = Arrays.asList(
        "8518750",  // The Battery, NY
        "8454000",  // Providence, RI
        "8575512",  // Annapolis, MD
        "8638610",  // Wilmington, NC
        "8658120",  // Charleston, SC
        "8720218",  // Mayport, FL
        "8726520",  // Miami Beach, FL
        "8729108",  // Panama City Beach, FL
        "8761724",  // Grand Isle, LA
        "8770570",  // Sabine Pass North, TX
        "9414290",  // San Francisco, CA
        "9447130",  // Seattle, WA
        "1612340",  // Honolulu, HI
        "9751364"   // San Juan, PR
    );

    /**
     * Fetch water level for a specific station
     */
    @CircuitBreaker(name = "noaa-tides-api", fallbackMethod = "fetchWaterLevelFallback")
    @Retry(name = "noaa-tides-api")
    @Cacheable(value = "noaa-tides", key = "#stationId", unless = "#result == null")
    public Mono<WaterLevelMetric> fetchWaterLevel(String stationId) {
        log.info("Fetching water level for NOAA station: {}", stationId);

        String url = String.format("%s/datagetter", baseUrl);
        
        WebClient webClient = webClientBuilder
            .baseUrl(url)
            .build();

        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .queryParam("station", stationId)
                .queryParam("product", "water_level")
                .queryParam("datum", "MLLW")  // Mean Lower Low Water
                .queryParam("units", "metric")
                .queryParam("time_zone", "gmt")
                .queryParam("application", application)
                .queryParam("format", "json")
                .queryParam("date", "latest")
                .build())
            .retrieve()
            .bodyToMono(NoaaTidesResponse.class)
            .map(response -> convertToMetric(response, stationId))
            .doOnSuccess(metric -> {
                if (metric != null) {
                    waterLevelRepository.save(metric);
                    log.info("Saved water level for station {}: {} meters", 
                        stationId, metric.getWaterLevelMeters());
                }
            })
            .doOnError(error -> log.error("Error fetching water level for station {}", stationId, error))
            .onErrorResume(error -> {
                log.error("Failed to fetch water level for station {}: {}", stationId, error.getMessage());
                return Mono.empty();
            });
    }

    /**
     * Fetch water levels for all monitoring stations
     */
    public Flux<WaterLevelMetric> fetchAllMonitoringStations() {
        log.info("Fetching water levels for {} monitoring stations", MONITORING_STATIONS.size());
        
        return Flux.fromIterable(MONITORING_STATIONS)
            .flatMap(stationId -> fetchWaterLevel(stationId)
                .delayElement(java.time.Duration.ofMillis(100))) // Rate limiting
            .doOnComplete(() -> log.info("Completed fetching all monitoring stations"));
    }

    /**
     * Convert NOAA API response to WaterLevelMetric entity
     */
    private WaterLevelMetric convertToMetric(NoaaTidesResponse response, String stationId) {
        try {
            if (response == null || response.getData() == null || response.getData().length == 0) {
                log.warn("No water level data found for station {}", stationId);
                return null;
            }

            NoaaTidesResponse.Data latestData = response.getData()[0];
            NoaaTidesResponse.Metadata metadata = response.getMetadata();

            // NOAA returns datetime without timezone: "2025-12-11 13:06"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            Instant timestamp = LocalDateTime.parse(latestData.getT(), formatter)
                .atZone(java.time.ZoneId.of("GMT"))
                .toInstant();
            double waterLevelMeters = Double.parseDouble(latestData.getV());
            double waterLevelFeet = waterLevelMeters * 3.28084; // Convert to feet

            return WaterLevelMetric.builder()
                .timestamp(timestamp)
                .stationId(stationId)
                .stationName(metadata != null ? metadata.getName() : stationId)
                .source("noaa_tides")
                .locationType(determineLocationType(stationId))
                .latitude(metadata != null ? metadata.getLat() : null)
                .longitude(metadata != null ? metadata.getLon() : null)
                .waterLevelMeters(waterLevelMeters)
                .waterLevelFeet(waterLevelFeet)
                .datum("MLLW")
                .qualityCode(latestData.getQ())
                .rawData(objectMapper.writeValueAsString(response))
                .processedAt(Instant.now())
                .build();

        } catch (Exception e) {
            log.error("Error converting NOAA response to metric for station {}", stationId, e);
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
            .datum(metric.getDatum())
            .floodSeverity(metric.getFloodSeverity())
            .isFlooding(metric.isFlooding())
            .qualityCode(metric.getQualityCode())
            .build();
    }

    /**
     * Determine location type based on station ID or name
     */
    private String determineLocationType(String stationId) {
        // NOAA CO-OPS stations are primarily ocean/coastal
        return "ocean";
    }

    /**
     * Fallback method when circuit breaker opens
     */
    private Mono<WaterLevelMetric> fetchWaterLevelFallback(String stationId, Exception e) {
        log.warn("Circuit breaker activated for NOAA Tides API (station {}), returning cached data", stationId, e);
        
        // Try to return latest cached data from database
        return Mono.fromCallable(() -> 
            waterLevelRepository.findFirstByStationIdOrderByTimestampDesc(stationId).orElse(null));
    }

    /**
     * Get list of all monitored station IDs
     */
    public List<String> getMonitoringStations() {
        return MONITORING_STATIONS;
    }
}
