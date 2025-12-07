package com.aiscientist.data_collector.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricDTO {
    
    private Long id;
    
    @NotNull
    private Instant timestamp;
    
    @NotNull
    private String source;
    
    @NotNull
    private String metricType;
    
    private Double kpIndex;
    private String cmeClass;
    private Integer speedKmh;
    private String rawData;
    private Instant processedAt;
}
