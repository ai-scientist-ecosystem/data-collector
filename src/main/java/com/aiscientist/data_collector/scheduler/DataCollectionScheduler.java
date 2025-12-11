package com.aiscientist.data_collector.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.aiscientist.data_collector.service.DataCollectorService;
import com.aiscientist.data_collector.service.EarthquakeCollectionService;
import com.aiscientist.data_collector.service.WaterLevelCollectionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataCollectionScheduler {

    private final DataCollectorService dataCollectorService;
    private final WaterLevelCollectionService waterLevelCollectionService;
    private final EarthquakeCollectionService earthquakeCollectionService;

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

    @Scheduled(cron = "${app.scheduler.noaa-tides.cron:0 */5 * * * *}")
    @ConditionalOnProperty(value = "app.scheduler.noaa-tides.enabled", havingValue = "true", matchIfMissing = true)
    public void scheduleNoaaTidesCollection() {
        log.info("Scheduled task: Collecting NOAA tides water level data");
        try {
            waterLevelCollectionService.collectNoaaTidesData();
        } catch (Exception e) {
            log.error("Error in scheduled NOAA tides collection", e);
        }
    }

    @Scheduled(cron = "${app.scheduler.usgs-water.cron:0 */10 * * * *}")
    @ConditionalOnProperty(value = "app.scheduler.usgs-water.enabled", havingValue = "true", matchIfMissing = true)
    public void scheduleUsgsWaterCollection() {
        log.info("Scheduled task: Collecting USGS river water level data");
        try {
            waterLevelCollectionService.collectUsgsWaterData();
        } catch (Exception e) {
            log.error("Error in scheduled USGS water collection", e);
        }
    }

    @Scheduled(cron = "${app.scheduler.earthquake.cron:0 */2 * * * *}")
    @ConditionalOnProperty(value = "app.scheduler.earthquake.enabled", havingValue = "true", matchIfMissing = true)
    public void scheduleEarthquakeCollection() {
        log.info("Scheduled task: Collecting earthquake data from USGS");
        try {
            earthquakeCollectionService.collectRecentEarthquakes();
        } catch (Exception e) {
            log.error("Error in scheduled earthquake collection", e);
        }
    }
}
