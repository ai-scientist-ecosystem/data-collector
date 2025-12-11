package com.aiscientist.data_collector.service;

import com.aiscientist.data_collector.dto.EarthquakeEvent;
import com.aiscientist.data_collector.dto.UsgsEarthquakeResponse;
import com.aiscientist.data_collector.model.EarthquakeMetric;
import com.aiscientist.data_collector.repository.EarthquakeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Service to fetch earthquake data from USGS Earthquake API
 * API Documentation: https://earthquake.usgs.gov/fdsnws/event/1/
 */
@Service
@Slf4j
public class UsgsEarthquakeApiService {

    private final WebClient webClient;
    private final EarthquakeRepository earthquakeRepository;
    private final ObjectMapper objectMapper;

    public UsgsEarthquakeApiService(
            @Qualifier("usgsWebClient") WebClient webClient,
            EarthquakeRepository earthquakeRepository,
            ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.earthquakeRepository = earthquakeRepository;
        this.objectMapper = objectMapper;
    }

    @Value("${app.usgs.earthquake.base-url:https://earthquake.usgs.gov}")
    private String baseUrl;

    @Value("${app.usgs.earthquake.min-magnitude:4.5}")
    private Double minMagnitude;

    private static final String EARTHQUAKE_ENDPOINT = "/fdsnws/event/1/query";

    /**
     * Fetch recent earthquakes from USGS API
     * 
     * @param hours Number of hours to look back
     * @param minMag Minimum magnitude threshold
     * @return Flux of earthquake metrics
     */
    @CircuitBreaker(name = "usgs-earthquake-api", fallbackMethod = "fetchEarthquakesFallback")
    @Retry(name = "usgs-earthquake-api")
    public Flux<EarthquakeMetric> fetchRecentEarthquakes(int hours, Double minMag) {
        log.info("Fetching earthquakes from last {} hours with magnitude >= {}", hours, minMag);

        String startTime = Instant.now()
                .minus(hours, ChronoUnit.HOURS)
                .toString();

        String queryParams = String.format("?format=geojson&starttime=%s&minmagnitude=%.1f&orderby=time",
                startTime, minMag);

        return webClient.get()
                .uri(baseUrl + EARTHQUAKE_ENDPOINT + queryParams)
                .retrieve()
                .bodyToMono(String.class)
                .flatMapMany(this::parseEarthquakeResponse)
                .flatMap(this::convertToMetric)
                .doOnNext(metric -> log.debug("Fetched earthquake: {} - M{} at {}",
                        metric.getEarthquakeId(), metric.getMagnitude(), metric.getLocation()))
                .doOnError(error -> log.error("Error fetching earthquakes from USGS", error));
    }

    /**
     * Fetch earthquakes with default settings (last 24 hours, magnitude >= configured minimum)
     */
    public Flux<EarthquakeMetric> fetchRecentEarthquakes() {
        return fetchRecentEarthquakes(24, minMagnitude);
    }

    /**
     * Fetch significant earthquakes (magnitude >= 6.0) from last 7 days
     */
    public Flux<EarthquakeMetric> fetchSignificantEarthquakes() {
        return fetchRecentEarthquakes(168, 6.0); // 7 days * 24 hours
    }

    /**
     * Fetch earthquakes near a specific location
     * 
     * @param latitude Center latitude
     * @param longitude Center longitude
     * @param radiusDegrees Search radius in degrees (1 degree ≈ 111 km)
     * @param minMag Minimum magnitude
     * @return Flux of earthquake metrics
     */
    @CircuitBreaker(name = "usgs-earthquake-api", fallbackMethod = "fetchEarthquakesFallback")
    @Retry(name = "usgs-earthquake-api")
    public Flux<EarthquakeMetric> fetchEarthquakesNearLocation(
            Double latitude, Double longitude, Double radiusDegrees, Double minMag) {
        
        log.info("Fetching earthquakes near ({}, {}) within {} degrees, magnitude >= {}",
                latitude, longitude, radiusDegrees, minMag);

        String startTime = Instant.now()
                .minus(30, ChronoUnit.DAYS)
                .toString();

        String url = String.format(
                "?format=geojson&starttime=%s&latitude=%.4f&longitude=%.4f&maxradiuskm=%.1f&minmagnitude=%.1f",
                startTime, latitude, longitude, radiusDegrees * 111.0, minMag);

        return webClient.get()
                .uri(baseUrl + EARTHQUAKE_ENDPOINT + url)
                .retrieve()
                .bodyToMono(String.class)
                .flatMapMany(this::parseEarthquakeResponse)
                .flatMap(this::convertToMetric)
                .doOnError(error -> log.error("Error fetching nearby earthquakes", error));
    }

    /**
     * Parse JSON response from USGS API
     */
    private Flux<UsgsEarthquakeResponse.EarthquakeFeature> parseEarthquakeResponse(String jsonResponse) {
        return Mono.fromCallable(() -> {
            UsgsEarthquakeResponse response = objectMapper.readValue(
                    jsonResponse, UsgsEarthquakeResponse.class);
            
            log.info("Parsed {} earthquake events from USGS", 
                    response.getFeatures() != null ? response.getFeatures().size() : 0);
            
            return response.getFeatures();
        })
        .flatMapMany(Flux::fromIterable)
        .onErrorResume(error -> {
            log.error("Failed to parse USGS earthquake response", error);
            return Flux.empty();
        });
    }

    /**
     * Convert USGS feature to EarthquakeMetric entity
     */
    private Mono<EarthquakeMetric> convertToMetric(UsgsEarthquakeResponse.EarthquakeFeature feature) {
        return Mono.fromCallable(() -> {
            UsgsEarthquakeResponse.Properties props = feature.getProperties();
            UsgsEarthquakeResponse.Geometry geom = feature.getGeometry();

            // Check if earthquake already exists in database
            Optional<EarthquakeMetric> existing = earthquakeRepository
                    .findByEarthquakeId(feature.getId());

            if (existing.isPresent()) {
                log.debug("Earthquake {} already exists, skipping", feature.getId());
                return existing.get();
            }

            // Extract coordinates [longitude, latitude, depth]
            Double longitude = geom.getCoordinates().get(0);
            Double latitude = geom.getCoordinates().get(1);
            Double depth = geom.getCoordinates().size() > 2 ? geom.getCoordinates().get(2) : null;

            // Determine region from place string
            String region = extractRegion(props.getPlace());

            EarthquakeMetric metric = EarthquakeMetric.builder()
                    .earthquakeId(feature.getId())
                    .magnitude(props.getMagnitude())
                    .magnitudeType(props.getMagnitudeType())
                    .depthKm(depth)
                    .latitude(latitude)
                    .longitude(longitude)
                    .eventTime(Instant.ofEpochMilli(props.getTime()))
                    .location(props.getPlace())
                    .region(region)
                    .tsunamiWarning(props.getTsunami() != null && props.getTsunami() == 1)
                    .alertLevel(props.getAlert())
                    .significance(props.getSignificance())
                    .feltReports(props.getFelt())
                    .maxIntensity(props.getMmi() != null ? String.valueOf(props.getMmi().intValue()) : null)
                    .dataSource(props.getNet())
                    .eventUrl(props.getUrl())
                    .collectedAt(Instant.now())
                    .build();

            // Save to database
            return earthquakeRepository.save(metric);
        })
        .doOnSuccess(metric -> log.debug("Converted and saved earthquake: {}", metric.getEarthquakeId()))
        .onErrorResume(error -> {
            log.error("Failed to convert earthquake feature", error);
            return Mono.empty();
        });
    }

    /**
     * Extract region/country from USGS place string
     * Example: "23 km SW of Tokyo, Japan" -> "Japan"
     */
    private String extractRegion(String place) {
        if (place == null || place.isEmpty()) {
            return "Unknown";
        }

        // Try to extract country/region after last comma
        int lastComma = place.lastIndexOf(',');
        if (lastComma >= 0 && lastComma < place.length() - 1) {
            return place.substring(lastComma + 1).trim();
        }

        // If no comma, use the full place string
        return place.trim();
    }

    /**
     * Create Kafka event from earthquake metric
     */
    public EarthquakeEvent createEvent(EarthquakeMetric metric) {
        return EarthquakeEvent.builder()
                .earthquakeId(metric.getEarthquakeId())
                .magnitude(metric.getMagnitude())
                .magnitudeType(metric.getMagnitudeType())
                .depthKm(metric.getDepthKm())
                .latitude(metric.getLatitude())
                .longitude(metric.getLongitude())
                .eventTime(metric.getEventTime())
                .location(metric.getLocation())
                .region(metric.getRegion())
                .severity(metric.getSeverity())
                .dangerous(metric.isDangerous())
                .catastrophic(metric.isCatastrophic())
                .shallow(metric.isShallow())
                .tsunamiWarning(metric.getTsunamiWarning())
                .tsunamiRiskScore(metric.getTsunamiRiskScore())
                .alertLevel(metric.getAlertLevel())
                .significance(metric.getSignificance())
                .feltReports(metric.getFeltReports())
                .dataSource(metric.getDataSource())
                .eventUrl(metric.getEventUrl())
                .collectedAt(metric.getCollectedAt())
                .eventType("earthquake.data")
                .build();
    }

    /**
     * Fallback method when USGS API is unavailable
     */
    private Flux<EarthquakeMetric> fetchEarthquakesFallback(Exception ex) {
        log.warn("USGS Earthquake API unavailable, using fallback: {}", ex.getMessage());
        
        // Return recent earthquakes from database
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        return Flux.fromIterable(earthquakeRepository.findRecentEarthquakes(cutoff))
                .doOnNext(metric -> log.debug("Returning cached earthquake: {}", metric.getEarthquakeId()));
    }

    /**
     * Fallback method for location-based queries
     */
    private Flux<EarthquakeMetric> fetchEarthquakesNearLocationFallback(
            Double latitude, Double longitude, Double radiusDegrees, Double minMag, Exception ex) {
        log.warn("USGS Earthquake API unavailable for location query, using fallback: {}", ex.getMessage());
        return Flux.empty();
    }
}
