package com.aiscientist.data_collector.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event DTO for earthquake data to be published to Kafka
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarthquakeEvent {

    /**
     * Unique earthquake identifier from USGS
     */
    private String earthquakeId;

    /**
     * Magnitude of the earthquake
     */
    private Double magnitude;

    /**
     * Magnitude type (mw, ml, mb, etc.)
     */
    private String magnitudeType;

    /**
     * Depth in kilometers
     */
    private Double depthKm;

    /**
     * Latitude of epicenter
     */
    private Double latitude;

    /**
     * Longitude of epicenter
     */
    private Double longitude;

    /**
     * Time when earthquake occurred
     */
    private Instant eventTime;

    /**
     * Human-readable location description
     */
    private String location;

    /**
     * Geographic region
     */
    private String region;

    /**
     * Severity level: MICRO, MINOR, LIGHT, MODERATE, STRONG, MAJOR, GREAT
     */
    private String severity;

    /**
     * Whether earthquake is dangerous (magnitude >= 5.0)
     */
    private Boolean dangerous;

    /**
     * Whether earthquake is catastrophic (magnitude >= 7.0)
     */
    private Boolean catastrophic;

    /**
     * Whether earthquake occurred in shallow depth (< 70km)
     */
    private Boolean shallow;

    /**
     * Tsunami warning flag from USGS
     */
    private Boolean tsunamiWarning;

    /**
     * Calculated tsunami risk score (0-100)
     */
    private Integer tsunamiRiskScore;

    /**
     * USGS alert level: green, yellow, orange, red
     */
    private String alertLevel;

    /**
     * USGS significance score (0-1000+)
     */
    private Integer significance;

    /**
     * Number of felt reports
     */
    private Integer feltReports;

    /**
     * Data source network
     */
    private String dataSource;

    /**
     * URL to USGS event details
     */
    private String eventUrl;

    /**
     * Timestamp when data was collected
     */
    private Instant collectedAt;

    /**
     * Event type: data or alert
     */
    private String eventType;
}
