package com.aiscientist.data_collector.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.aiscientist.data_collector.model.Metric;
import com.aiscientist.data_collector.repository.MetricRepository;
import com.aiscientist.data_collector.service.DataCollectorService;

@WebMvcTest(DataCollectorController.class)
class DataCollectorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataCollectorService dataCollectorService;

    @MockBean
    private MetricRepository metricRepository;

    @Test
    void triggerKpIndexCollection_shouldReturn200() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/collector/collect/kp-index"))
                .andExpect(status().isOk())
                .andExpect(content().string("Kp index data collection triggered"));
        
        verify(dataCollectorService, times(1)).collectKpIndexData();
    }

    @Test
    void triggerCMECollection_shouldReturn200() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/collector/collect/cme"))
                .andExpect(status().isOk())
                .andExpect(content().string("CME data collection triggered"));
        
        verify(dataCollectorService, times(1)).collectCMEData();
    }

    @Test
    void getRecentMetrics_shouldReturnMetrics() throws Exception {
        // Given
        Metric metric = Metric.builder()
                .id(1L)
                .timestamp(Instant.now())
                .source("noaa")
                .metricType("kp_index")
                .kpIndex(3.0)
                .build();
        
        when(metricRepository.findRecentMetrics(anyString(), any(Instant.class)))
                .thenReturn(List.of(metric));
        
        // When & Then
        mockMvc.perform(get("/api/v1/collector/metrics")
                .param("source", "noaa")
                .param("hours", "24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].source").value("noaa"))
                .andExpect(jsonPath("$[0].metricType").value("kp_index"));
    }

    @Test
    void health_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/collector/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Data Collector is running"));
    }
}
