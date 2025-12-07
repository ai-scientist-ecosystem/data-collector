package com.aiscientist.data_collector.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.aiscientist.data_collector.config.AppConfig;
import com.aiscientist.data_collector.dto.CMEEvent;
import com.aiscientist.data_collector.dto.KpIndexEvent;
import com.aiscientist.data_collector.exception.ExternalApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoaaApiService {

    private final WebClient noaaWebClient;
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;

    @CircuitBreaker(name = "noaa-api", fallbackMethod = "getKpIndexFallback")
    @Retry(name = "noaa-api")
    @Cacheable(value = "kp-index", unless = "#result == null || #result.isEmpty()")
    public Flux<KpIndexEvent> fetchKpIndexData() {
        log.info("Fetching Kp index data from NOAA");
        
        return noaaWebClient.get()
                .uri(appConfig.getNoaa().getApi().getKpIndexUrl())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMapMany(response -> {
                    try {
                        return Flux.fromIterable(response)
                                .map(node -> {
                                    try {
                                        String rawData = objectMapper.writeValueAsString(node);
                                        return KpIndexEvent.builder()
                                                .timeTag(node.get("time_tag").asText())
                                                .kpIndex(node.has("Kp") ? node.get("Kp").asDouble() : null)
                                                .estimatedKp(node.has("estimated_Kp") ? 
                                                        node.get("estimated_Kp").asDouble() : null)
                                                .source("noaa")
                                                .timestamp(Instant.now())
                                                .rawData(rawData)
                                                .build();
                                    } catch (Exception e) {
                                        log.error("Error parsing Kp index data: {}", node, e);
                                        return null;
                                    }
                                })
                                .filter(event -> event != null);
                    } catch (Exception e) {
                        log.error("Error processing NOAA response", e);
                        return Flux.error(new ExternalApiException("NOAA", e.getMessage(), e));
                    }
                })
                .doOnError(error -> log.error("Error fetching Kp index data", error))
                .onErrorResume(error -> {
                    throw new ExternalApiException("NOAA", error.getMessage(), error);
                });
    }

    private Flux<KpIndexEvent> getKpIndexFallback(Exception e) {
        log.warn("Circuit breaker activated for NOAA API, returning empty data", e);
        return Flux.empty();
    }
}
