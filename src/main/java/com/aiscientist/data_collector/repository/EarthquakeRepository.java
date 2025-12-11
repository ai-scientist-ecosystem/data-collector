package com.aiscientist.data_collector.repository;

import com.aiscientist.data_collector.model.EarthquakeMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for earthquake metrics
 */
@Repository
public interface EarthquakeRepository extends JpaRepository<EarthquakeMetric, Long> {

    /**
     * Find earthquake by USGS event ID
     */
    Optional<EarthquakeMetric> findByEarthquakeId(String earthquakeId);

    /**
     * Find earthquakes with magnitude greater than or equal to threshold
     */
    List<EarthquakeMetric> findByMagnitudeGreaterThanEqualOrderByEventTimeDesc(Double magnitude);

    /**
     * Find recent earthquakes since a given time
     */
    @Query("SELECT e FROM EarthquakeMetric e WHERE e.eventTime >= :since ORDER BY e.eventTime DESC")
    List<EarthquakeMetric> findRecentEarthquakes(@Param("since") Instant since);

    /**
     * Find earthquakes in a specific region
     */
    List<EarthquakeMetric> findByRegionContainingIgnoreCaseOrderByEventTimeDesc(String region);

    /**
     * Find earthquakes with tsunami warning
     */
    List<EarthquakeMetric> findByTsunamiWarningTrueOrderByEventTimeDesc();

    /**
     * Find dangerous earthquakes (magnitude >= 5.0)
     */
    @Query("SELECT e FROM EarthquakeMetric e WHERE e.magnitude >= 5.0 ORDER BY e.eventTime DESC")
    List<EarthquakeMetric> findDangerousEarthquakes();

    /**
     * Find catastrophic earthquakes (magnitude >= 7.0)
     */
    @Query("SELECT e FROM EarthquakeMetric e WHERE e.magnitude >= 7.0 ORDER BY e.eventTime DESC")
    List<EarthquakeMetric> findCatastrophicEarthquakes();

    /**
     * Find shallow earthquakes (depth < 70km) which cause more surface damage
     */
    @Query("SELECT e FROM EarthquakeMetric e WHERE e.depthKm < 70.0 ORDER BY e.eventTime DESC")
    List<EarthquakeMetric> findShallowEarthquakes();

    /**
     * Find earthquakes by alert level
     */
    List<EarthquakeMetric> findByAlertLevelOrderByEventTimeDesc(String alertLevel);

    /**
     * Find earthquakes near a location (within a bounding box)
     * @param minLat Minimum latitude
     * @param maxLat Maximum latitude
     * @param minLon Minimum longitude
     * @param maxLon Maximum longitude
     */
    @Query("SELECT e FROM EarthquakeMetric e WHERE " +
           "e.latitude BETWEEN :minLat AND :maxLat AND " +
           "e.longitude BETWEEN :minLon AND :maxLon " +
           "ORDER BY e.eventTime DESC")
    List<EarthquakeMetric> findEarthquakesInBoundingBox(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon);

    /**
     * Count earthquakes by severity level in a time range
     */
    @Query("SELECT COUNT(e) FROM EarthquakeMetric e WHERE " +
           "e.eventTime >= :since AND e.magnitude >= :minMag")
    Long countEarthquakesSince(@Param("since") Instant since, @Param("minMag") Double minMag);

    /**
     * Get latest earthquake
     */
    Optional<EarthquakeMetric> findFirstByOrderByEventTimeDesc();

    /**
     * Count earthquakes with tsunami warning in time range
     */
    @Query("SELECT COUNT(e) FROM EarthquakeMetric e WHERE " +
           "e.eventTime >= :since AND e.tsunamiWarning = true")
    Long countTsunamiWarnings(@Param("since") Instant since);

    /**
     * Find earthquakes by data source network
     */
    List<EarthquakeMetric> findByDataSourceOrderByEventTimeDesc(String dataSource);
}
