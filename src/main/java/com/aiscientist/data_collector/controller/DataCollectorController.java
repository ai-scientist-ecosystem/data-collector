package com.aiscientist.data_collector.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aiscientist.data_collector.dto.MetricDTO;
import com.aiscientist.data_collector.repository.MetricRepository;
import com.aiscientist.data_collector.service.DataCollectorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/collector")
@RequiredArgsConstructor
@Slf4j
public class DataCollectorController {

    private final DataCollectorService dataCollectorService;
    private final MetricRepository metricRepository;

    @PostMapping("/collect/kp-index")
    public ResponseEntity<String> triggerKpIndexCollection() {
        log.info("Manual trigger: Kp index data collection");
        dataCollectorService.collectKpIndexData();
        return ResponseEntity.ok("Kp index data collection triggered");
    }

    @PostMapping("/collect/cme")
    public ResponseEntity<String> triggerCMECollection() {
        log.info("Manual trigger: CME data collection");
        dataCollectorService.collectCMEData();
        return ResponseEntity.ok("CME data collection triggered");
    }

    @GetMapping("/metrics")
    public ResponseEntity<List<MetricDTO>> getRecentMetrics(
            @RequestParam(defaultValue = "noaa") String source,
            @RequestParam(defaultValue = "24") int hours) {
        
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        
        List<MetricDTO> metrics = metricRepository.findRecentMetrics(source, since)
                .stream()
                .map(metric -> MetricDTO.builder()
                        .id(metric.getId())
                        .timestamp(metric.getTimestamp())
                        .source(metric.getSource())
                        .metricType(metric.getMetricType())
                        .kpIndex(metric.getKpIndex())
                        .cmeClass(metric.getCmeClass())
                        .speedKmh(metric.getSpeedKmh())
                        .processedAt(metric.getProcessedAt())
                        .build())
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Data Collector is running");
    }
}
