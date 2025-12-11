package com.aiscientist.data_collector.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for NOAA CO-OPS Tides and Currents API response
 * API Endpoint: https://api.tidesandcurrents.noaa.gov/api/prod/datagetter
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoaaTidesResponse {

    private Metadata metadata;
    private Data[] data;

    @lombok.Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {
        private String id;           // Station ID
        private String name;         // Station name
        private Double lat;
        private Double lon;
    }

    @lombok.Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data {
        private String t;            // Timestamp (ISO 8601)
        private String v;            // Water level value
        private String s;            // Sigma (standard deviation)
        private String f;            // Flags/quality
        private String q;            // Quality assurance
    }
}
