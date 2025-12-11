package com.aiscientist.data_collector.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Response DTO for USGS Earthquake API
 * API: https://earthquake.usgs.gov/fdsnws/event/1/query
 * Format: GeoJSON
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UsgsEarthquakeResponse {

    /**
     * Type of GeoJSON object (always "FeatureCollection")
     */
    private String type;

    /**
     * Metadata about the query
     */
    private Metadata metadata;

    /**
     * List of earthquake events (features)
     */
    private List<EarthquakeFeature> features;

    /**
     * Bounding box of all earthquakes [minLon, minLat, minDepth, maxLon, maxLat, maxDepth]
     */
    private List<Double> bbox;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metadata {
        /**
         * Time query was generated
         */
        private Long generated;

        /**
         * URL of the request
         */
        private String url;

        /**
         * Title describing the data
         */
        private String title;

        /**
         * HTTP status code
         */
        private Integer status;

        /**
         * API version
         */
        private String api;

        /**
         * Number of earthquake events returned
         */
        private Integer count;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EarthquakeFeature {
        /**
         * Type of GeoJSON object (always "Feature")
         */
        private String type;

        /**
         * Earthquake properties
         */
        private Properties properties;

        /**
         * Geographic information
         */
        private Geometry geometry;

        /**
         * Unique earthquake identifier
         */
        private String id;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Properties {
        /**
         * Magnitude of the earthquake
         */
        @JsonProperty("mag")
        private Double magnitude;

        /**
         * Human-readable location
         * Example: "23 km SW of Tokyo, Japan"
         */
        private String place;

        /**
         * Time of the earthquake in milliseconds since epoch
         */
        private Long time;

        /**
         * Last update time in milliseconds
         */
        private Long updated;

        /**
         * Time zone offset in minutes
         */
        private Integer tz;

        /**
         * Link to USGS event page
         */
        private String url;

        /**
         * Link to detailed GeoJSON
         */
        private String detail;

        /**
         * Number of seismic stations that reported the event
         */
        private Integer felt;

        /**
         * Community-reported intensity (Modified Mercalli Intensity)
         */
        private Double cdi;

        /**
         * Maximum reported intensity (Modified Mercalli Intensity)
         */
        private Double mmi;

        /**
         * Alert level: green, yellow, orange, red
         */
        private String alert;

        /**
         * Review status: automatic, reviewed
         */
        private String status;

        /**
         * Flag for tsunami warning (1 = yes, 0 = no)
         */
        private Integer tsunami;

        /**
         * Significance score (0-1000+)
         * Combines magnitude, felt reports, and estimated impact
         */
        @JsonProperty("sig")
        private Integer significance;

        /**
         * Network that contributed the event
         */
        private String net;

        /**
         * Event code from the contributing network
         */
        private String code;

        /**
         * Identifier sources (comma-separated)
         */
        private String ids;

        /**
         * Sources that contributed magnitude values
         */
        private String sources;

        /**
         * Event type: earthquake, quarry, explosion, etc.
         */
        private String type;

        /**
         * Magnitude type: mw (moment), ml (local), mb (body-wave), etc.
         */
        @JsonProperty("magType")
        private String magnitudeType;

        /**
         * Number of seismic stations used
         */
        private Integer nst;

        /**
         * Horizontal distance from epicenter to nearest station (degrees)
         */
        private Double dmin;

        /**
         * RMS travel time residual (seconds)
         */
        private Double rms;

        /**
         * Largest azimuthal gap between stations (degrees)
         */
        private Double gap;

        /**
         * Title summarizing the event
         */
        private String title;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Geometry {
        /**
         * Type of geometry (always "Point")
         */
        private String type;

        /**
         * Coordinates: [longitude, latitude, depth in km]
         */
        private List<Double> coordinates;
    }
}
