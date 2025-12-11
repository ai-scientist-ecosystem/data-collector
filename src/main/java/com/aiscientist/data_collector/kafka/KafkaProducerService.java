package com.aiscientist.data_collector.kafka;

import org.springframework.stereotype.Service;

/**
 * Alias for SpaceWeatherProducer to provide clearer naming for general Kafka operations
 */
@Service
public class KafkaProducerService extends SpaceWeatherProducer {
    
    public KafkaProducerService(org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate) {
        super(kafkaTemplate);
    }
}
