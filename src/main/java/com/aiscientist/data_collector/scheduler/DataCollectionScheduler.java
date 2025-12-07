package com.aiscientist.data_collector.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.aiscientist.data_collector.service.DataCollectorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataCollectionScheduler {

    private final DataCollectorService dataCollectorService;

    @Scheduled(cron = "${app.scheduler.noaa-kp.cron}")
    @ConditionalOnProperty(value = "app.scheduler.noaa-kp.enabled", havingValue = "true", matchIfMissing = true)
    public void scheduleKpIndexCollection() {
        log.info("Scheduled task: Collecting Kp index data");
        try {
            dataCollectorService.collectKpIndexData();
        } catch (Exception e) {
            log.error("Error in scheduled Kp index collection", e);
        }
    }

    @Scheduled(cron = "${app.scheduler.nasa-cme.cron}")
    @ConditionalOnProperty(value = "app.scheduler.nasa-cme.enabled", havingValue = "true", matchIfMissing = true)
    public void scheduleCMECollection() {
        log.info("Scheduled task: Collecting CME data");
        try {
            dataCollectorService.collectCMEData();
        } catch (Exception e) {
            log.error("Error in scheduled CME collection", e);
        }
    }
}
