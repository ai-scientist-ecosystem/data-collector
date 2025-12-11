package com.aiscientist.data_collector.model;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "water_level_metrics", indexes = {
    @Index(name = "idx_station_timestamp", columnList = "station_id,timestamp"),
    @Index(name = "idx_location_type", columnList = "location_type,timestamp"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterLevelMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false, name = "station_id", length = 50)
    private String stationId; // NOAA station ID or USGS site number

    @Column(nullable = false, name = "station_name", length = 255)
    private String stationName;

    @Column(nullable = false, length = 20)
    private String source; // 'noaa_tides', 'usgs_water'

    @Column(nullable = false, name = "location_type", length = 20)
    private String locationType; // 'ocean', 'river', 'lake', 'estuary'

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "water_level_meters")
    private Double waterLevelMeters; // Current water level in meters

    @Column(name = "water_level_feet")
    private Double waterLevelFeet; // Current water level in feet

    @Column(name = "datum", length = 50)
    private String datum; // Reference datum (MLLW, MSL, NAVD88, etc.)

    @Column(name = "discharge_cfs")
    private Double dischargeCfs; // Stream discharge in cubic feet per second (USGS only)

    @Column(name = "gage_height_feet")
    private Double gageHeightFeet; // Gage height in feet (USGS only)

    @Column(name = "flood_stage_feet")
    private Double floodStageFeet; // Official flood stage threshold

    @Column(name = "action_stage_feet")
    private Double actionStageFeet; // Action stage threshold

    @Column(name = "minor_flood_stage_feet")
    private Double minorFloodStageFeet;

    @Column(name = "moderate_flood_stage_feet")
    private Double moderateFloodStageFeet;

    @Column(name = "major_flood_stage_feet")
    private Double majorFloodStageFeet;

    @Column(name = "quality_code", length = 10)
    private String qualityCode; // Data quality indicator

    @Column(name = "raw_data", columnDefinition = "TEXT")
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

    /**
     * Check if current water level exceeds any flood threshold
     */
    public String getFloodSeverity() {
        if (waterLevelFeet == null) return "NORMAL";
        
        if (majorFloodStageFeet != null && waterLevelFeet >= majorFloodStageFeet) {
            return "MAJOR";
        }
        if (moderateFloodStageFeet != null && waterLevelFeet >= moderateFloodStageFeet) {
            return "MODERATE";
        }
        if (minorFloodStageFeet != null && waterLevelFeet >= minorFloodStageFeet) {
            return "MINOR";
        }
        if (actionStageFeet != null && waterLevelFeet >= actionStageFeet) {
            return "ACTION";
        }
        
        return "NORMAL";
    }

    /**
     * Check if location is currently in flood condition
     */
    public boolean isFlooding() {
        return !"NORMAL".equals(getFloodSeverity());
    }
}
