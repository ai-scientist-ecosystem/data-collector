package com.aiscientist.data_collector.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event DTO for publishing water level events to Kafka
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterLevelEvent {

    private String stationId;
    private String stationName;
    private String source;
    private String locationType;
    private Double latitude;
    private Double longitude;
    private Instant timestamp;
    private Double waterLevelMeters;
    private Double waterLevelFeet;
    private String datum;
    private Double dischargeCfs;
    private Double gageHeightFeet;
    private Double floodStageFeet;
    private String floodSeverity;
    private boolean isFlooding;
    private String qualityCode;
}
