package com.aiscientist.data_collector.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aiscientist.data_collector.dto.CMEEvent;
import com.aiscientist.data_collector.dto.KpIndexEvent;
import com.aiscientist.data_collector.kafka.SpaceWeatherProducer;
import com.aiscientist.data_collector.repository.MetricRepository;

import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class DataCollectorServiceTest {

    @Mock
    private NoaaApiService noaaApiService;
    
    @Mock
    private NasaApiService nasaApiService;
    
    @Mock
    private SpaceWeatherProducer spaceWeatherProducer;
    
    @Mock
    private MetricRepository metricRepository;
    
    @InjectMocks
    private DataCollectorService dataCollectorService;

    @Test
    void collectKpIndexData_shouldProcessAndSaveData() throws InterruptedException {
        // Given
        KpIndexEvent event = KpIndexEvent.builder()
                .timeTag("2024-12-07T00:00:00Z")
                .kpIndex(3.0)
                .source("noaa")
                .build();
        
        when(noaaApiService.fetchKpIndexData()).thenReturn(Flux.just(event));
        when(metricRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When - collectKpIndexData() is async, need to wait for completion
        dataCollectorService.collectKpIndexData();
        Thread.sleep(100); // Allow async processing to complete
        
        // Then
        verify(noaaApiService, times(1)).fetchKpIndexData();
        verify(spaceWeatherProducer, times(1)).sendKpIndexEvent(any());
    }

    @Test
    void collectCMEData_shouldProcessAndSaveData() throws InterruptedException {
        // Given
        CMEEvent event = CMEEvent.builder()
                .activityId("2024-12-07-CME-001")
                .speed(500)
                .source("nasa")
                .build();
        
        when(nasaApiService.fetchCMEData()).thenReturn(Flux.just(event));
        when(metricRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When - collectCMEData() is async, need to wait for completion
        dataCollectorService.collectCMEData();
        Thread.sleep(100); // Allow async processing to complete
        
        // Then
        verify(nasaApiService, times(1)).fetchCMEData();
        verify(spaceWeatherProducer, times(1)).sendCMEEvent(any());
    }
}
