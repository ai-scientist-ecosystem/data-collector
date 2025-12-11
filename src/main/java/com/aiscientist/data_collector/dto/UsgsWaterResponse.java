package com.aiscientist.data_collector.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for USGS Water Services Instantaneous Values API response
 * API Endpoint: https://waterservices.usgs.gov/nwis/iv/
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsgsWaterResponse {

    private Value value;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Value {
        private TimeSeries[] timeSeries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeries {
        private SourceInfo sourceInfo;
        private Variable variable;
        private TimeSeriesValue[] values;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceInfo {
        private String siteCode;
        private String siteName;
        private GeoLocation geoLocation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoLocation {
        private GeogLocation geogLocation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeogLocation {
        private Double latitude;
        private Double longitude;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Variable {
        private String variableCode;
        private String variableName;
        private String variableDescription;
        private String unit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesValue {
        private DataValue[] value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataValue {
        private String value;
        private String[] qualifiers;
        private String dateTime;
    }
}
