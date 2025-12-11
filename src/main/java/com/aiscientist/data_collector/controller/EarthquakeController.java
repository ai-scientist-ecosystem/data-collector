package com.aiscientist.data_collector.controller;

import com.aiscientist.data_collector.model.EarthquakeMetric;
import com.aiscientist.data_collector.repository.EarthquakeRepository;
import com.aiscientist.data_collector.service.EarthquakeCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for earthquake monitoring
 */
@RestController
@RequestMapping("/api/v1/earthquake")
@RequiredArgsConstructor
@Slf4j
public class EarthquakeController {

    private final EarthquakeCollectionService earthquakeCollectionService;
    private final EarthquakeRepository earthquakeRepository;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "earthquake-monitoring-api"
        ));
    }

    /**
     * Trigger manual collection of recent earthquakes
     */
    @PostMapping("/collect")
    public ResponseEntity<Map<String, String>> collectRecentEarthquakes() {
        log.info("Manual earthquake collection triggered");
        earthquakeCollectionService.collectRecentEarthquakes();
        return ResponseEntity.ok(Map.of(
                "status", "Collection started",
                "message", "Fetching recent earthquakes (last 24h, magnitude >= 4.5)"
        ));
    }

    /**
     * Trigger collection of significant earthquakes
     */
    @PostMapping("/collect/significant")
    public ResponseEntity<Map<String, String>> collectSignificantEarthquakes() {
        log.info("Manual significant earthquake collection triggered");
        earthquakeCollectionService.collectSignificantEarthquakes();
        return ResponseEntity.ok(Map.of(
                "status", "Collection started",
                "message", "Fetching significant earthquakes (last 7 days, magnitude >= 6.0)"
        ));
    }

    /**
     * Trigger collection of earthquakes near a location
     */
    @PostMapping("/collect/location")
    public ResponseEntity<Map<String, String>> collectNearLocation(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5.0") Double radiusDegrees,
            @RequestParam(defaultValue = "4.0") Double minMagnitude) {
        
        log.info("Manual location-based collection triggered: ({}, {})", latitude, longitude);
        earthquakeCollectionService.collectEarthquakesNearLocation(
                latitude, longitude, radiusDegrees, minMagnitude);
        
        return ResponseEntity.ok(Map.of(
                "status", "Collection started",
                "message", String.format("Fetching earthquakes near (%.4f, %.4f)", latitude, longitude)
        ));
    }

    /**
     * Get recent earthquakes
     */
    @GetMapping("/recent")
    public ResponseEntity<List<EarthquakeMetric>> getRecentEarthquakes(
            @RequestParam(defaultValue = "24") int hours) {
        
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<EarthquakeMetric> earthquakes = earthquakeRepository.findRecentEarthquakes(since);
        
        log.info("Retrieved {} earthquakes from last {} hours", earthquakes.size(), hours);
        return ResponseEntity.ok(earthquakes);
    }

    /**
     * Get earthquakes by minimum magnitude
     */
    @GetMapping("/magnitude/{minMagnitude}")
    public ResponseEntity<List<EarthquakeMetric>> getEarthquakesByMagnitude(
            @PathVariable Double minMagnitude) {
        
        List<EarthquakeMetric> earthquakes = earthquakeRepository
                .findByMagnitudeGreaterThanEqualOrderByEventTimeDesc(minMagnitude);
        
        log.info("Retrieved {} earthquakes with magnitude >= {}", earthquakes.size(), minMagnitude);
        return ResponseEntity.ok(earthquakes);
    }

    /**
     * Get dangerous earthquakes (magnitude >= 5.0)
     */
    @GetMapping("/dangerous")
    public ResponseEntity<List<EarthquakeMetric>> getDangerousEarthquakes() {
        List<EarthquakeMetric> earthquakes = earthquakeRepository.findDangerousEarthquakes();
        log.info("Retrieved {} dangerous earthquakes", earthquakes.size());
        return ResponseEntity.ok(earthquakes);
    }

    /**
     * Get catastrophic earthquakes (magnitude >= 7.0)
     */
    @GetMapping("/catastrophic")
    public ResponseEntity<List<EarthquakeMetric>> getCatastrophicEarthquakes() {
        List<EarthquakeMetric> earthquakes = earthquakeRepository.findCatastrophicEarthquakes();
        log.info("Retrieved {} catastrophic earthquakes", earthquakes.size());
        return ResponseEntity.ok(earthquakes);
    }

    /**
     * Get earthquakes with tsunami warnings
     */
    @GetMapping("/tsunami-warnings")
    public ResponseEntity<List<EarthquakeMetric>> getTsunamiWarnings() {
        List<EarthquakeMetric> earthquakes = earthquakeRepository.findByTsunamiWarningTrueOrderByEventTimeDesc();
        log.info("Retrieved {} earthquakes with tsunami warnings", earthquakes.size());
        return ResponseEntity.ok(earthquakes);
    }

    /**
     * Get earthquakes in a specific region
     */
    @GetMapping("/region/{regionName}")
    public ResponseEntity<List<EarthquakeMetric>> getEarthquakesByRegion(
            @PathVariable String regionName) {
        
        List<EarthquakeMetric> earthquakes = earthquakeRepository
                .findByRegionContainingIgnoreCaseOrderByEventTimeDesc(regionName);
        
        log.info("Retrieved {} earthquakes in region: {}", earthquakes.size(), regionName);
        return ResponseEntity.ok(earthquakes);
    }

    /**
     * Get earthquakes by alert level
     */
    @GetMapping("/alert/{alertLevel}")
    public ResponseEntity<List<EarthquakeMetric>> getEarthquakesByAlertLevel(
            @PathVariable String alertLevel) {
        
        List<EarthquakeMetric> earthquakes = earthquakeRepository
                .findByAlertLevelOrderByEventTimeDesc(alertLevel);
        
        log.info("Retrieved {} earthquakes with alert level: {}", earthquakes.size(), alertLevel);
        return ResponseEntity.ok(earthquakes);
    }

    /**
     * Get earthquakes near a location (within bounding box)
     */
    @GetMapping("/location/nearby")
    public ResponseEntity<List<EarthquakeMetric>> getNearbyEarthquakes(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5.0") Double radiusDegrees) {
        
        // Calculate bounding box
        Double minLat = latitude - radiusDegrees;
        Double maxLat = latitude + radiusDegrees;
        Double minLon = longitude - radiusDegrees;
        Double maxLon = longitude + radiusDegrees;
        
        List<EarthquakeMetric> earthquakes = earthquakeRepository.findEarthquakesInBoundingBox(
                minLat, maxLat, minLon, maxLon);
        
        log.info("Retrieved {} earthquakes near ({}, {}) within {}°", 
                earthquakes.size(), latitude, longitude, radiusDegrees);
        return ResponseEntity.ok(earthquakes);
    }

    /**
     * Get shallow earthquakes (depth < 70km) which cause more surface damage
     */
    @GetMapping("/shallow")
    public ResponseEntity<List<EarthquakeMetric>> getShallowEarthquakes() {
        List<EarthquakeMetric> earthquakes = earthquakeRepository.findShallowEarthquakes();
        log.info("Retrieved {} shallow earthquakes", earthquakes.size());
        return ResponseEntity.ok(earthquakes);
    }

    /**
     * Get latest earthquake
     */
    @GetMapping("/latest")
    public ResponseEntity<EarthquakeMetric> getLatestEarthquake() {
        return earthquakeRepository.findFirstByOrderByEventTimeDesc()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get earthquake by USGS event ID
     */
    @GetMapping("/{earthquakeId}")
    public ResponseEntity<EarthquakeMetric> getEarthquakeById(
            @PathVariable String earthquakeId) {
        
        return earthquakeRepository.findByEarthquakeId(earthquakeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get earthquake monitoring statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Instant last24h = Instant.now().minus(24, ChronoUnit.HOURS);
        Instant last7days = Instant.now().minus(7, ChronoUnit.DAYS);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEarthquakes", earthquakeRepository.count());
        stats.put("last24Hours", earthquakeRepository.countEarthquakesSince(last24h, 0.0));
        stats.put("last7Days", earthquakeRepository.countEarthquakesSince(last7days, 0.0));
        stats.put("dangerous24h", earthquakeRepository.countEarthquakesSince(last24h, 5.0));
        stats.put("catastrophic7days", earthquakeRepository.countEarthquakesSince(last7days, 7.0));
        stats.put("tsunamiWarnings24h", earthquakeRepository.countTsunamiWarnings(last24h));
        stats.put("timestamp", Instant.now());
        
        earthquakeRepository.findFirstByOrderByEventTimeDesc().ifPresent(latest -> {
            stats.put("latestEarthquake", Map.of(
                    "earthquakeId", latest.getEarthquakeId(),
                    "magnitude", latest.getMagnitude(),
                    "location", latest.getLocation(),
                    "eventTime", latest.getEventTime(),
                    "severity", latest.getSeverity()
            ));
        });
        
        log.info("Retrieved earthquake statistics");
        return ResponseEntity.ok(stats);
    }

    /**
     * Get high tsunami risk earthquakes
     */
    @GetMapping("/tsunami-risk/high")
    public ResponseEntity<List<Map<String, Object>>> getHighTsunamiRiskEarthquakes() {
        List<EarthquakeMetric> allEarthquakes = earthquakeRepository
                .findRecentEarthquakes(Instant.now().minus(30, ChronoUnit.DAYS));
        
        List<Map<String, Object>> highRiskEarthquakes = allEarthquakes.stream()
                .filter(eq -> eq.getTsunamiRiskScore() >= 50)
                .map(eq -> Map.of(
                        "earthquakeId", (Object) eq.getEarthquakeId(),
                        "magnitude", eq.getMagnitude(),
                        "location", eq.getLocation(),
                        "tsunamiRiskScore", eq.getTsunamiRiskScore(),
                        "tsunamiWarning", eq.getTsunamiWarning() != null ? eq.getTsunamiWarning() : false,
                        "eventTime", eq.getEventTime()
                ))
                .toList();
        
        log.info("Retrieved {} high tsunami risk earthquakes", highRiskEarthquakes.size());
        return ResponseEntity.ok(highRiskEarthquakes);
    }
}
