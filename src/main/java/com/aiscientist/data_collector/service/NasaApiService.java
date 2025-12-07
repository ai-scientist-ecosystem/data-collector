package com.aiscientist.data_collector.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.aiscientist.data_collector.config.AppConfig;
import com.aiscientist.data_collector.dto.CMEEvent;
import com.aiscientist.data_collector.exception.ExternalApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
@Slf4j
public class NasaApiService {

    private final WebClient nasaWebClient;
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;

    @CircuitBreaker(name = "nasa-api", fallbackMethod = "getCMEDataFallback")
    @Retry(name = "nasa-api")
    @Cacheable(value = "cme-data", unless = "#result == null || #result.isEmpty()")
    public Flux<CMEEvent> fetchCMEData() {
        log.info("Fetching CME data from NASA DONKI");
        
        String startDate = LocalDate.now().minusDays(7).format(DateTimeFormatter.ISO_DATE);
        String endDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        
        String url = String.format("%s%s?startDate=%s&endDate=%s&api_key=%s",
                appConfig.getNasa().getApi().getDonkiUrl(),
                appConfig.getNasa().getApi().getCmeEndpoint(),
                startDate,
                endDate,
                appConfig.getNasa().getApi().getKey());
        
        return nasaWebClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMapMany(response -> {
                    try {
                        return Flux.fromIterable(response)
                                .map(node -> {
                                    try {
                                        String rawData = objectMapper.writeValueAsString(node);
                                        CMEEvent.CMEEventBuilder builder = CMEEvent.builder()
                                                .activityId(node.has("activityID") ? 
                                                        node.get("activityID").asText() : null)
                                                .startTime(node.has("startTime") ? 
                                                        node.get("startTime").asText() : null)
                                                .sourceLocation(node.has("sourceLocation") ? 
                                                        node.get("sourceLocation").asText() : null)
                                                .catalog(node.has("catalog") ? 
                                                        node.get("catalog").asText() : null)
                                                .source("nasa")
                                                .timestamp(Instant.now())
                                                .rawData(rawData);
                                        
                                        // Extract speed from first analysis if available
                                        if (node.has("cmeAnalyses") && node.get("cmeAnalyses").size() > 0) {
                                            JsonNode firstAnalysis = node.get("cmeAnalyses").get(0);
                                            if (firstAnalysis.has("speed")) {
                                                builder.speed(firstAnalysis.get("speed").asInt());
                                            }
                                            if (firstAnalysis.has("type")) {
                                                builder.type(firstAnalysis.get("type").asText());
                                            }
                                        }
                                        
                                        return builder.build();
                                    } catch (Exception e) {
                                        log.error("Error parsing CME data: {}", node, e);
                                        return null;
                                    }
                                })
                                .filter(event -> event != null);
                    } catch (Exception e) {
                        log.error("Error processing NASA response", e);
                        return Flux.error(new ExternalApiException("NASA", e.getMessage(), e));
                    }
                })
                .doOnError(error -> log.error("Error fetching CME data", error))
                .onErrorResume(error -> {
                    throw new ExternalApiException("NASA", error.getMessage(), error);
                });
    }

    private Flux<CMEEvent> getCMEDataFallback(Exception e) {
        log.warn("Circuit breaker activated for NASA API, returning empty data", e);
        return Flux.empty();
    }
}
