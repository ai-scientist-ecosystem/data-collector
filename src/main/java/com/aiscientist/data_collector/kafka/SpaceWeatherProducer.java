package com.aiscientist.data_collector.kafka;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import com.aiscientist.data_collector.dto.CMEEvent;
import com.aiscientist.data_collector.dto.EarthquakeEvent;
import com.aiscientist.data_collector.dto.KpIndexEvent;
import com.aiscientist.data_collector.dto.WaterLevelEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpaceWeatherProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${app.kafka.topics.raw-space-weather-kp}")
    private String kpIndexTopic;
    
    @Value("${app.kafka.topics.raw-space-weather-cme}")
    private String cmeTopic;

    public void sendKpIndexEvent(KpIndexEvent event) {
        log.debug("Publishing Kp index event to topic: {}", kpIndexTopic);
        
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(kpIndexTopic, event.getTimeTag(), event);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Kp index event published successfully: topic={}, offset={}, key={}", 
                         kpIndexTopic, 
                         result.getRecordMetadata().offset(),
                         event.getTimeTag());
            } else {
                log.error("Failed to publish Kp index event: {}", event, ex);
            }
        });
    }

    public void sendCMEEvent(CMEEvent event) {
        log.debug("Publishing CME event to topic: {}", cmeTopic);
        
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(cmeTopic, event.getActivityId(), event);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("CME event published successfully: topic={}, offset={}, key={}", 
                         cmeTopic, 
                         result.getRecordMetadata().offset(),
                         event.getActivityId());
            } else {
                log.error("Failed to publish CME event: {}", event, ex);
            }
        });
    }

    public void sendWaterLevelEvent(String topic, WaterLevelEvent event) {
        log.debug("Publishing water level event to topic: {}", topic);
        
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(topic, event.getStationId(), event);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Water level event published successfully: topic={}, station={}, flooding={}, severity={}", 
                         topic, 
                         event.getStationId(),
                         event.isFlooding(),
                         event.getFloodSeverity());
            } else {
                log.error("Failed to publish water level event: {}", event, ex);
            }
        });
    }

    public void sendEarthquakeEvent(String topic, EarthquakeEvent event) {
        log.debug("Publishing earthquake event to topic: {}", topic);
        
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(topic, event.getEarthquakeId(), event);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Earthquake event published successfully: topic={}, id={}, magnitude={}, severity={}, tsunami_risk={}", 
                         topic, 
                         event.getEarthquakeId(),
                         event.getMagnitude(),
                         event.getSeverity(),
                         event.getTsunamiRiskScore());
            } else {
                log.error("Failed to publish earthquake event: {}", event, ex);
            }
        });
    }
}
