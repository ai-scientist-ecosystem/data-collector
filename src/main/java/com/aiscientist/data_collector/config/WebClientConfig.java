package com.aiscientist.data_collector.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient nasaWebClient(AppConfig config) {
        return WebClient.builder()
                .baseUrl(config.getNasa().getApi().getBaseUrl())
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public WebClient noaaWebClient(AppConfig config) {
        return WebClient.builder()
                .baseUrl(config.getNoaa().getApi().getBaseUrl())
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public WebClient usgsWebClient(
            @Value("${app.usgs.earthquake.base-url:https://earthquake.usgs.gov}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
