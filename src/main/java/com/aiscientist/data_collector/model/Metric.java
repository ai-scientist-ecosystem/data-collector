package com.aiscientist.data_collector.model;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "metrics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Metric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false, length = 50)
    private String source; // 'nasa', 'noaa'

    @Column(nullable = false, length = 50, name = "metric_type")
    private String metricType; // 'kp_index', 'cme', 'solar_flare'

    @Column(name = "kp_index")
    private Double kpIndex;

    @Column(name = "cme_class", length = 10)
    private String cmeClass;

    @Column(name = "speed_kmh")
    private Integer speedKmh;

    @Column(name = "raw_data", columnDefinition = "jsonb")
    private String rawData;

    @Column(name = "processed_at")
    private Instant processedAt;

    @PrePersist
    protected void onCreate() {
        if (processedAt == null) {
            processedAt = Instant.now();
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
