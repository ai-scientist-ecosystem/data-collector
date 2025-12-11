package com.aiscientist.data_collector.service;

import com.aiscientist.data_collector.dto.WaterLevelEvent;
import com.aiscientist.data_collector.kafka.KafkaProducerService;
import com.aiscientist.data_collector.model.WaterLevelMetric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Service to orchestrate water level data collection from multiple sources
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WaterLevelCollectionService {

    private final NoaaTidesApiService noaaTidesApiService;
    private final UsgsWaterApiService usgsWaterApiService;
    private final KafkaProducerService kafkaProducerService;

    private static final String WATER_LEVEL_TOPIC = "raw.waterlevel.data";
    private static final String FLOOD_ALERT_TOPIC = "raw.flood.alert";

    /**
     * Collect water level data from NOAA Tides & Currents API
     */
    public void collectNoaaTidesData() {
        log.info("Starting NOAA tides water level collection");
        
        noaaTidesApiService.fetchAllMonitoringStations()
            .doOnNext(metric -> {
                // Publish to Kafka
                WaterLevelEvent event = noaaTidesApiService.createEvent(metric);
                kafkaProducerService.sendWaterLevelEvent(WATER_LEVEL_TOPIC, event);
                
                // If flooding detected, send alert
                if (metric.isFlooding()) {
                    kafkaProducerService.sendWaterLevelEvent(FLOOD_ALERT_TOPIC, event);
                    log.warn("FLOOD ALERT: Station {} - {} severity", 
                        metric.getStationId(), metric.getFloodSeverity());
                }
            })
            .doOnComplete(() -> log.info("Completed NOAA tides water level collection"))
            .doOnError(error -> log.error("Error during NOAA tides collection", error))
            .subscribe();
    }

    /**
     * Collect water level data from USGS Water Services API
     */
    public void collectUsgsWaterData() {
        log.info("Starting USGS river water level collection");
        
        usgsWaterApiService.fetchAllMonitoringSites()
            .doOnNext(metric -> {
                // Publish to Kafka
                WaterLevelEvent event = usgsWaterApiService.createEvent(metric);
                kafkaProducerService.sendWaterLevelEvent(WATER_LEVEL_TOPIC, event);
                
                // If flooding detected, send alert
                if (metric.isFlooding()) {
                    kafkaProducerService.sendWaterLevelEvent(FLOOD_ALERT_TOPIC, event);
                    log.warn("FLOOD ALERT: Site {} - {} severity", 
                        metric.getStationId(), metric.getFloodSeverity());
                }
            })
            .doOnComplete(() -> log.info("Completed USGS river water level collection"))
            .doOnError(error -> log.error("Error during USGS water collection", error))
            .subscribe();
    }

    /**
     * Collect water level for a specific station/site
     */
    public void collectSpecificStation(String source, String stationId) {
        log.info("Collecting water level for {} station: {}", source, stationId);
        
        Flux<WaterLevelMetric> result;
        
        if ("noaa_tides".equalsIgnoreCase(source)) {
            result = noaaTidesApiService.fetchWaterLevel(stationId).flux();
        } else if ("usgs_water".equalsIgnoreCase(source)) {
            result = usgsWaterApiService.fetchWaterData(stationId).flux();
        } else {
            log.error("Unknown source: {}", source);
            return;
        }
        
        result.doOnNext(metric -> {
                WaterLevelEvent event = source.contains("noaa") 
                    ? noaaTidesApiService.createEvent(metric)
                    : usgsWaterApiService.createEvent(metric);
                kafkaProducerService.sendWaterLevelEvent(WATER_LEVEL_TOPIC, event);
                
                if (metric.isFlooding()) {
                    kafkaProducerService.sendWaterLevelEvent(FLOOD_ALERT_TOPIC, event);
                }
            })
            .subscribe();
    }
}
