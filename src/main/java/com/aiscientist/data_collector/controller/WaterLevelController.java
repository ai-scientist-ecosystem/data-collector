package com.aiscientist.data_collector.controller;

import com.aiscientist.data_collector.model.WaterLevelMetric;
import com.aiscientist.data_collector.repository.WaterLevelMetricRepository;
import com.aiscientist.data_collector.service.NoaaTidesApiService;
import com.aiscientist.data_collector.service.UsgsWaterApiService;
import com.aiscientist.data_collector.service.WaterLevelCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for water level and flood monitoring
 */
@RestController
@RequestMapping("/api/v1/water-level")
@RequiredArgsConstructor
@Slf4j
public class WaterLevelController {

    private final WaterLevelMetricRepository waterLevelRepository;
    private final WaterLevelCollectionService waterLevelCollectionService;
    private final NoaaTidesApiService noaaTidesApiService;
    private final UsgsWaterApiService usgsWaterApiService;

    /**
     * Manual trigger to collect water level data from all sources
     */
    @PostMapping("/collect/all")
    public ResponseEntity<Map<String, String>> collectAll() {
        log.info("Manual trigger: Collecting all water level data");
        
        waterLevelCollectionService.collectNoaaTidesData();
        waterLevelCollectionService.collectUsgsWaterData();
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Water level collection started for all sources"
        ));
    }

    /**
     * Manual trigger to collect NOAA tides data
     */
    @PostMapping("/collect/noaa-tides")
    public ResponseEntity<Map<String, String>> collectNoaaTides() {
        log.info("Manual trigger: Collecting NOAA tides data");
        
        waterLevelCollectionService.collectNoaaTidesData();
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "NOAA tides collection started"
        ));
    }

    /**
     * Manual trigger to collect USGS river data
     */
    @PostMapping("/collect/usgs-water")
    public ResponseEntity<Map<String, String>> collectUsgsWater() {
        log.info("Manual trigger: Collecting USGS river data");
        
        waterLevelCollectionService.collectUsgsWaterData();
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "USGS water collection started"
        ));
    }

    /**
     * Get all stations currently in flood condition
     */
    @GetMapping("/flooding")
    public ResponseEntity<List<WaterLevelMetric>> getFloodingStations() {
        log.info("Fetching all stations currently flooding");
        
        List<WaterLevelMetric> flooding = waterLevelRepository.findCurrentlyFlooding();
        
        return ResponseEntity.ok(flooding);
    }

    /**
     * Get latest water level for a specific station
     */
    @GetMapping("/station/{stationId}/latest")
    public ResponseEntity<WaterLevelMetric> getLatestWaterLevel(
            @PathVariable String stationId) {
        log.info("Fetching latest water level for station: {}", stationId);
        
        return waterLevelRepository.findFirstByStationIdOrderByTimestampDesc(stationId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get water level history for a specific station
     */
    @GetMapping("/station/{stationId}/history")
    public ResponseEntity<List<WaterLevelMetric>> getWaterLevelHistory(
            @PathVariable String stationId,
            @RequestParam(required = false, defaultValue = "24") int hours) {
        log.info("Fetching water level history for station {} (last {} hours)", stationId, hours);
        
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<WaterLevelMetric> history = waterLevelRepository
            .findByStationIdAndTimestampBetweenOrderByTimestampDesc(stationId, since, Instant.now());
        
        return ResponseEntity.ok(history);
    }

    /**
     * Get water levels by location type
     */
    @GetMapping("/type/{locationType}")
    public ResponseEntity<List<WaterLevelMetric>> getByLocationType(
            @PathVariable String locationType,
            @RequestParam(required = false, defaultValue = "24") int hours) {
        log.info("Fetching water levels for location type: {}", locationType);
        
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<WaterLevelMetric> metrics = waterLevelRepository
            .findByLocationTypeAndTimestampBetweenOrderByTimestampDesc(locationType, since, Instant.now());
        
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get water levels by source
     */
    @GetMapping("/source/{source}")
    public ResponseEntity<List<WaterLevelMetric>> getBySource(
            @PathVariable String source,
            @RequestParam(required = false, defaultValue = "24") int hours) {
        log.info("Fetching water levels from source: {}", source);
        
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<WaterLevelMetric> metrics = waterLevelRepository
            .findBySourceAndTimestampAfterOrderByTimestampDesc(source, since);
        
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get stations near a geographic location
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<WaterLevelMetric>> getStationsNearby(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "1.0") double radiusDegrees) {
        log.info("Fetching stations near lat={}, lon={}, radius={}", latitude, longitude, radiusDegrees);
        
        double minLat = latitude - radiusDegrees;
        double maxLat = latitude + radiusDegrees;
        double minLon = longitude - radiusDegrees;
        double maxLon = longitude + radiusDegrees;
        
        List<WaterLevelMetric> stations = waterLevelRepository
            .findStationsInBoundingBox(minLat, maxLat, minLon, maxLon);
        
        return ResponseEntity.ok(stations);
    }

    /**
     * Get statistics about water level monitoring
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        log.info("Fetching water level monitoring statistics");
        
        Instant last30Min = Instant.now().minus(30, ChronoUnit.MINUTES);
        long activeStations = waterLevelRepository.countActiveStationsSince(last30Min);
        long floodingCount = waterLevelRepository.findCurrentlyFlooding().size();
        
        return ResponseEntity.ok(Map.of(
            "activeStations", activeStations,
            "currentlyFlooding", floodingCount,
            "noaaStations", noaaTidesApiService.getMonitoringStations().size(),
            "usgsStations", usgsWaterApiService.getMonitoringSites().size(),
            "timestamp", Instant.now()
        ));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "water-level-api"
        ));
    }
}
