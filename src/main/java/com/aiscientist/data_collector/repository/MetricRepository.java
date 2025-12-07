package com.aiscientist.data_collector.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.aiscientist.data_collector.model.Metric;

@Repository
public interface MetricRepository extends JpaRepository<Metric, Long> {

    List<Metric> findBySourceAndMetricType(String source, String metricType);

    List<Metric> findByTimestampBetween(Instant start, Instant end);

    @Query("SELECT m FROM Metric m WHERE m.source = :source AND m.timestamp >= :since ORDER BY m.timestamp DESC")
    List<Metric> findRecentMetrics(@Param("source") String source, @Param("since") Instant since);

    @Query("SELECT m FROM Metric m WHERE m.kpIndex >= :threshold AND m.timestamp >= :since")
    List<Metric> findHighKpIndexMetrics(@Param("threshold") Double threshold, @Param("since") Instant since);
}
