package com.aiscientist.data_collector.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CMEEvent {
    
    @JsonProperty("activity_id")
    private String activityId;
    
    @JsonProperty("start_time")
    private String startTime;
    
    private Integer speed;
    
    private String type;
    
    @JsonProperty("source_location")
    private String sourceLocation;
    
    @JsonProperty("catalog")
    private String catalog;
    
    @JsonProperty("cme_analyses")
    private List<CMEAnalysis> cmeAnalyses;
    
    private String source;
    private Instant timestamp;
    
    @JsonProperty("raw_data")
    private String rawData;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CMEAnalysis {
        private String time21_5;
        private Double latitude;
        private Double longitude;
        private Integer speed;
        private String type;
        private Boolean isMostAccurate;
        private String note;
    }
}
