package com.aiscientist.data_collector.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity representing earthquake event data
 * Data source: USGS Earthquake API
 */
@Entity
@Table(name = "earthquake_metrics", indexes = {
    @Index(name = "idx_earthquake_id", columnList = "earthquake_id", unique = true),
    @Index(name = "idx_event_time", columnList = "event_time"),
    @Index(name = "idx_magnitude", columnList = "magnitude"),
    @Index(name = "idx_tsunami_warning", columnList = "tsunami_warning")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarthquakeMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * USGS unique event identifier (e.g., "us7000m123")
     */
    @Column(name = "earthquake_id", unique = true, nullable = false)
    private String earthquakeId;

    /**
     * Earthquake magnitude (Richter/Moment Magnitude Scale)
     * Range: typically 0.0 - 10.0
     */
    @Column(name = "magnitude", nullable = false)
    private Double magnitude;

    /**
     * Magnitude type (e.g., "mw" = moment magnitude, "ml" = local magnitude)
     */
    @Column(name = "magnitude_type")
    private String magnitudeType;

    /**
     * Depth of earthquake hypocenter in kilometers
     */
    @Column(name = "depth_km")
    private Double depthKm;

    /**
     * Latitude of epicenter (-90 to 90)
     */
    @Column(name = "latitude", nullable = false)
    private Double latitude;

    /**
     * Longitude of epicenter (-180 to 180)
     */
    @Column(name = "longitude", nullable = false)
    private Double longitude;

    /**
     * Time of earthquake occurrence
     */
    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    /**
     * Human-readable location description
     * Example: "23 km SW of Tokyo, Japan"
     */
    @Column(name = "location", length = 500)
    private String location;

    /**
     * Location country or region
     */
    @Column(name = "region")
    private String region;

    /**
     * Whether this earthquake triggered a tsunami warning
     */
    @Column(name = "tsunami_warning")
    private Boolean tsunamiWarning;

    /**
     * USGS alert level: green, yellow, orange, red
     */
    @Column(name = "alert_level")
    private String alertLevel;

    /**
     * USGS significance score (0-1000+)
     * Combines magnitude, felt reports, and estimated impact
     */
    @Column(name = "significance")
    private Integer significance;

    /**
     * Number of reported felt experiences
     */
    @Column(name = "felt_reports")
    private Integer feltReports;

    /**
     * Modified Mercalli Intensity (I-XII)
     */
    @Column(name = "max_intensity")
    private String maxIntensity;

    /**
     * Data source (e.g., "us", "ci", "nc")
     */
    @Column(name = "data_source")
    private String dataSource;

    /**
     * URL to USGS event page
     */
    @Column(name = "event_url", length = 1000)
    private String eventUrl;

    /**
     * Timestamp when data was collected
     */
    @Column(name = "collected_at")
    private Instant collectedAt;

    @PrePersist
    protected void onCreate() {
        if (collectedAt == null) {
            collectedAt = Instant.now();
        }
    }

    /**
     * Calculate severity level based on magnitude
     * 
     * @return Severity classification: MICRO, MINOR, LIGHT, MODERATE, STRONG, MAJOR, GREAT
     */
    public String getSeverity() {
        if (magnitude == null) {
            return "UNKNOWN";
        }
        
        if (magnitude >= 8.0) {
            return "GREAT";        // Global catastrophe, rare (1/year)
        } else if (magnitude >= 7.0) {
            return "MAJOR";        // Serious damage over large areas (20/year)
        } else if (magnitude >= 6.0) {
            return "STRONG";       // Significant damage (100/year)
        } else if (magnitude >= 5.0) {
            return "MODERATE";     // Minor building damage (1,000/year)
        } else if (magnitude >= 4.0) {
            return "LIGHT";        // Felt, objects shake (10,000/year)
        } else if (magnitude >= 3.0) {
            return "MINOR";        // Often felt, rarely damage (100,000/year)
        } else {
            return "MICRO";        // Not felt, recorded by seismographs
        }
    }

    /**
     * Check if earthquake is dangerous (magnitude >= 5.0)
     */
    public boolean isDangerous() {
        return magnitude != null && magnitude >= 5.0;
    }

    /**
     * Check if earthquake is potentially catastrophic (magnitude >= 7.0)
     */
    public boolean isCatastrophic() {
        return magnitude != null && magnitude >= 7.0;
    }

    /**
     * Check if earthquake occurred in shallow depth (< 70km)
     * Shallow earthquakes cause more surface damage
     */
    public boolean isShallow() {
        return depthKm != null && depthKm < 70.0;
    }

    /**
     * Calculate tsunami risk score (0-100)
     * Based on magnitude, depth, and location (oceanic)
     */
    public int getTsunamiRiskScore() {
        if (magnitude == null || depthKm == null) {
            return 0;
        }
        
        // Tsunami risk factors:
        // 1. Magnitude >= 6.5 (undersea megathrust earthquakes)
        // 2. Shallow depth (< 70km)
        // 3. Tsunami warning flag from USGS
        
        int risk = 0;
        
        if (magnitude >= 7.5) {
            risk += 50;
        } else if (magnitude >= 6.5) {
            risk += 30;
        }
        
        if (depthKm < 30) {
            risk += 25;
        } else if (depthKm < 70) {
            risk += 15;
        }
        
        if (Boolean.TRUE.equals(tsunamiWarning)) {
            risk += 25;
        }
        
        return Math.min(risk, 100);
    }
}
