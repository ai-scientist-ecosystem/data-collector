package com.aiscientist.data_collector.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiscientist.data_collector.dto.CMEEvent;
import com.aiscientist.data_collector.dto.KpIndexEvent;
import com.aiscientist.data_collector.kafka.SpaceWeatherProducer;
import com.aiscientist.data_collector.model.Metric;
import com.aiscientist.data_collector.repository.MetricRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataCollectorService {

    private final NoaaApiService noaaApiService;
    private final NasaApiService nasaApiService;
    private final SpaceWeatherProducer spaceWeatherProducer;
    private final MetricRepository metricRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void collectKpIndexData() {
        log.info("Starting Kp index data collection");
        
        noaaApiService.fetchKpIndexData()
                .doOnNext(event -> {
                    // Save to database
                    Metric metric = Metric.builder()
                            .timestamp(Instant.parse(event.getTimeTag()))
                            .source("noaa")
                            .metricType("kp_index")
                            .kpIndex(event.getKpIndex() != null ? event.getKpIndex() : event.getEstimatedKp())
                            .rawData(event.getRawData())
                            .processedAt(Instant.now())
                            .build();
                    
                    metricRepository.save(metric);
                    log.debug("Saved Kp index metric: {}", metric.getId());
                    
                    // Publish to Kafka
                    spaceWeatherProducer.sendKpIndexEvent(event);
                })
                .doOnComplete(() -> log.info("Kp index data collection completed"))
                .doOnError(error -> log.error("Error collecting Kp index data", error))
                .subscribe();
    }

    @Transactional
    public void collectCMEData() {
        log.info("Starting CME data collection");
        
        nasaApiService.fetchCMEData()
                .doOnNext(event -> {
                    // Save to database
                    Metric metric = Metric.builder()
                            .timestamp(Instant.now())
                            .source("nasa")
                            .metricType("cme")
                            .speedKmh(event.getSpeed())
                            .cmeClass(event.getType())
                            .rawData(event.getRawData())
                            .processedAt(Instant.now())
                            .build();
                    
                    metricRepository.save(metric);
                    log.debug("Saved CME metric: {}", metric.getId());
                    
                    // Publish to Kafka
                    spaceWeatherProducer.sendCMEEvent(event);
                })
                .doOnComplete(() -> log.info("CME data collection completed"))
                .doOnError(error -> log.error("Error collecting CME data", error))
                .subscribe();
    }
}
