package com.aiscientist.data_collector.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import com.aiscientist.data_collector.config.AppConfig;
import com.aiscientist.data_collector.dto.KpIndexEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class NoaaApiServiceTest {

    @Mock
    private WebClient noaaWebClient;
    
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    
    @Mock
    private WebClient.ResponseSpec responseSpec;
    
    @Mock
    private AppConfig appConfig;
    
    @InjectMocks
    private NoaaApiService noaaApiService;
    
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        AppConfig.NoaaConfig noaaConfig = new AppConfig.NoaaConfig();
        AppConfig.NoaaConfig.ApiConfig apiConfig = new AppConfig.NoaaConfig.ApiConfig();
        apiConfig.setKpIndexUrl("https://services.swpc.noaa.gov/json/planetary_k_index_1m.json");
        noaaConfig.setApi(apiConfig);
        
        when(appConfig.getNoaa()).thenReturn(noaaConfig);
    }

    @Test
    void fetchKpIndexData_shouldReturnKpIndexEvents() throws Exception {
        // Given
        String jsonResponse = "[{\"time_tag\":\"2024-12-07T00:00:00Z\",\"Kp\":3.0,\"estimated_Kp\":null}]";
        ArrayNode arrayNode = (ArrayNode) objectMapper.readTree(jsonResponse);
        
        when(noaaWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(arrayNode));
        
        // When
        Flux<KpIndexEvent> result = noaaApiService.fetchKpIndexData();
        
        // Then
        StepVerifier.create(result)
                .assertNext(event -> {
                    assertNotNull(event);
                    assertEquals("2024-12-07T00:00:00Z", event.getTimeTag());
                    assertEquals(3.0, event.getKpIndex());
                    assertEquals("noaa", event.getSource());
                    assertNotNull(event.getTimestamp());
                })
                .verifyComplete();
    }
}
