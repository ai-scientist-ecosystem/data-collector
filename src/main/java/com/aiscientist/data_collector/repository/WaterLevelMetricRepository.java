package com.aiscientist.data_collector.repository;

import com.aiscientist.data_collector.model.WaterLevelMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface WaterLevelMetricRepository extends JpaRepository<WaterLevelMetric, Long> {

    /**
     * Find latest water level for a specific station
     */
    Optional<WaterLevelMetric> findFirstByStationIdOrderByTimestampDesc(String stationId);

    /**
     * Find all water levels for a station within time range
     */
    List<WaterLevelMetric> findByStationIdAndTimestampBetweenOrderByTimestampDesc(
        String stationId, Instant start, Instant end);

    /**
     * Find all recent water levels from a specific source
     */
    List<WaterLevelMetric> findBySourceAndTimestampAfterOrderByTimestampDesc(
        String source, Instant since);

    /**
     * Find all stations currently in flood condition
     */
    @Query("SELECT w FROM WaterLevelMetric w WHERE " +
           "w.id IN (SELECT MAX(w2.id) FROM WaterLevelMetric w2 GROUP BY w2.stationId) " +
           "AND (w.waterLevelFeet >= w.minorFloodStageFeet " +
           "OR w.waterLevelFeet >= w.moderateFloodStageFeet " +
           "OR w.waterLevelFeet >= w.majorFloodStageFeet)")
    List<WaterLevelMetric> findCurrentlyFlooding();

    /**
     * Find all water levels by location type in time range
     */
    List<WaterLevelMetric> findByLocationTypeAndTimestampBetweenOrderByTimestampDesc(
        String locationType, Instant start, Instant end);

    /**
     * Count stations reporting in last N minutes
     */
    @Query("SELECT COUNT(DISTINCT w.stationId) FROM WaterLevelMetric w " +
           "WHERE w.timestamp >= :since")
    long countActiveStationsSince(@Param("since") Instant since);

    /**
     * Find stations near a geographic location
     */
    @Query("SELECT w FROM WaterLevelMetric w WHERE " +
           "w.id IN (SELECT MAX(w2.id) FROM WaterLevelMetric w2 GROUP BY w2.stationId) " +
           "AND w.latitude BETWEEN :minLat AND :maxLat " +
           "AND w.longitude BETWEEN :minLon AND :maxLon")
    List<WaterLevelMetric> findStationsInBoundingBox(
        @Param("minLat") double minLat, 
        @Param("maxLat") double maxLat,
        @Param("minLon") double minLon, 
        @Param("maxLon") double maxLon);
}
