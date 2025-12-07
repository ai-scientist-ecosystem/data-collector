package com.aiscientist.data_collector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppConfig {
    
    private NasaConfig nasa;
    private NoaaConfig noaa;
    private SchedulerConfig scheduler;
    private KafkaTopicsConfig kafka;
    private CacheConfig cache;
    
    @Data
    public static class NasaConfig {
        private ApiConfig api;
        
        @Data
        public static class ApiConfig {
            private String baseUrl;
            private String key;
            private String donkiUrl;
            private String cmeEndpoint;
            private String solarFlareEndpoint;
        }
    }
    
    @Data
    public static class NoaaConfig {
        private ApiConfig api;
        
        @Data
        public static class ApiConfig {
            private String baseUrl;
            private String kpIndexUrl;
            private String solarWindUrl;
        }
    }
    
    @Data
    public static class SchedulerConfig {
        private CronConfig nasaCme;
        private CronConfig noaaKp;
        
        @Data
        public static class CronConfig {
            private String cron;
            private boolean enabled;
        }
    }
    
    @Data
    public static class KafkaTopicsConfig {
        private TopicsConfig topics;
        
        @Data
        public static class TopicsConfig {
            private String rawSpaceWeatherKp;
            private String rawSpaceWeatherCme;
            private String rawSpaceWeatherSolarFlare;
        }
    }
    
    @Data
    public static class CacheConfig {
        private TtlConfig ttl;
        
        @Data
        public static class TtlConfig {
            private int kpIndex;
            private int cmeData;
        }
    }
}
