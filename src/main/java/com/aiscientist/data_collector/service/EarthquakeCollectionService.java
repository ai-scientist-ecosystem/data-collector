package com.aiscientist.data_collector.service;

import com.aiscientist.data_collector.dto.EarthquakeEvent;
import com.aiscientist.data_collector.kafka.KafkaProducerService;
import com.aiscientist.data_collector.model.EarthquakeMetric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to orchestrate earthquake data collection and event publishing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EarthquakeCollectionService {

    private final UsgsEarthquakeApiService usgsEarthquakeApiService;
    private final KafkaProducerService kafkaProducerService;

    private static final String EARTHQUAKE_DATA_TOPIC = "raw.earthquake.data";
    private static final String EARTHQUAKE_ALERT_TOPIC = "raw.earthquake.alert";
    private static final String TSUNAMI_WARNING_TOPIC = "raw.tsunami.warning";

    /**
     * Collect recent earthquakes from USGS API (last 24 hours, magnitude >= 4.5)
     */
    public void collectRecentEarthquakes() {
        log.info("Starting earthquake data collection");

        usgsEarthquakeApiService.fetchRecentEarthquakes()
                .doOnNext(metric -> {
                    // Publish all earthquake data to main topic
                    EarthquakeEvent event = usgsEarthquakeApiService.createEvent(metric);
                    kafkaProducerService.sendEarthquakeEvent(EARTHQUAKE_DATA_TOPIC, event);

                    // If dangerous (magnitude >= 5.0), send alert
                    if (metric.isDangerous()) {
                        event.setEventType("earthquake.alert");
                        kafkaProducerService.sendEarthquakeEvent(EARTHQUAKE_ALERT_TOPIC, event);
                        log.warn("EARTHQUAKE ALERT: M{} - {} - {} severity - {}",
                                metric.getMagnitude(),
                                metric.getEarthquakeId(),
                                metric.getSeverity(),
                                metric.getLocation());
                    }

                    // If tsunami warning or high tsunami risk, send tsunami alert
                    if (Boolean.TRUE.equals(metric.getTsunamiWarning()) || metric.getTsunamiRiskScore() >= 50) {
                        event.setEventType("tsunami.warning");
                        kafkaProducerService.sendEarthquakeEvent(TSUNAMI_WARNING_TOPIC, event);
                        log.error("TSUNAMI WARNING: M{} at {} - Risk Score: {} - {}",
                                metric.getMagnitude(),
                                metric.getDepthKm() != null ? metric.getDepthKm() + "km depth" : "unknown depth",
                                metric.getTsunamiRiskScore(),
                                metric.getLocation());
                    }
                })
                .doOnComplete(() -> log.info("Completed earthquake data collection"))
                .doOnError(error -> log.error("Error during earthquake collection", error))
                .subscribe();
    }

    /**
     * Collect significant earthquakes (magnitude >= 6.0) from last 7 days
     */
    public void collectSignificantEarthquakes() {
        log.info("Starting significant earthquake collection");

        usgsEarthquakeApiService.fetchSignificantEarthquakes()
                .doOnNext(metric -> {
                    EarthquakeEvent event = usgsEarthquakeApiService.createEvent(metric);
                    event.setEventType("earthquake.significant");
                    kafkaProducerService.sendEarthquakeEvent(EARTHQUAKE_ALERT_TOPIC, event);

                    log.warn("SIGNIFICANT EARTHQUAKE: M{} - {} severity - {}",
                            metric.getMagnitude(), metric.getSeverity(), metric.getLocation());

                    // Check tsunami risk
                    if (metric.getTsunamiRiskScore() >= 50) {
                        event.setEventType("tsunami.warning");
                        kafkaProducerService.sendEarthquakeEvent(TSUNAMI_WARNING_TOPIC, event);
                    }
                })
                .doOnComplete(() -> log.info("Completed significant earthquake collection"))
                .doOnError(error -> log.error("Error during significant earthquake collection", error))
                .subscribe();
    }

    /**
     * Collect earthquakes near a specific location
     *
     * @param latitude Center latitude
     * @param longitude Center longitude
     * @param radiusDegrees Search radius in degrees (1 degree ≈ 111 km)
     * @param minMagnitude Minimum magnitude threshold
     */
    public void collectEarthquakesNearLocation(
            Double latitude, Double longitude, Double radiusDegrees, Double minMagnitude) {
        
        log.info("Collecting earthquakes near ({}, {}) within {}° radius, magnitude >= {}",
                latitude, longitude, radiusDegrees, minMagnitude);

        usgsEarthquakeApiService.fetchEarthquakesNearLocation(latitude, longitude, radiusDegrees, minMagnitude)
                .doOnNext(metric -> {
                    EarthquakeEvent event = usgsEarthquakeApiService.createEvent(metric);
                    event.setEventType("earthquake.location");
                    kafkaProducerService.sendEarthquakeEvent(EARTHQUAKE_DATA_TOPIC, event);

                    if (metric.isDangerous()) {
                        kafkaProducerService.sendEarthquakeEvent(EARTHQUAKE_ALERT_TOPIC, event);
                    }
                })
                .doOnComplete(() -> log.info("Completed location-based earthquake collection"))
                .doOnError(error -> log.error("Error during location-based earthquake collection", error))
                .subscribe();
    }
}
