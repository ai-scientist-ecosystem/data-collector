package com.aiscientist.data_collector.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpIndexEvent {
    
    @JsonProperty("time_tag")
    private String timeTag;
    
    @JsonProperty("kp_index")
    private Double kpIndex;
    
    @JsonProperty("estimated_kp")
    private Double estimatedKp;
    
    private String source;
    private Instant timestamp;
    
    @JsonProperty("raw_data")
    private String rawData;
}
