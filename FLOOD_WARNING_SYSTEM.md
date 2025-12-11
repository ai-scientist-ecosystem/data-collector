# Flood Warning System

## 🌊 Overview

The **Flood Warning System** is an extension to the Data Collector service that monitors water levels from oceans, rivers, and streams worldwide using NOAA and USGS APIs. It provides real-time flood detection and alerts based on official flood stage thresholds.

### Key Features
- ✅ **Real-time ocean tide monitoring** from NOAA CO-OPS (14 coastal stations)
- ✅ **River/stream water level monitoring** from USGS (13 major rivers)
- ✅ **Automatic flood detection** based on official NWS flood stages
- ✅ **Multi-severity flood alerts** (ACTION → MINOR → MODERATE → MAJOR)
- ✅ **Geographic search** for stations near any location
- ✅ **Historical data** tracking and analysis
- ✅ **Kafka event streaming** for real-time alerts
- ✅ **Global coverage** (3,000+ NOAA stations, 25,000+ USGS sites available)

---

## 🗺️ Monitored Locations

### NOAA Tides & Currents (Ocean/Coastal)
- **The Battery, NY** (8518750) - New York Harbor
- **Providence, RI** (8454000) - Narragansett Bay
- **Annapolis, MD** (8575512) - Chesapeake Bay
- **Wilmington, NC** (8638610) - Cape Fear River
- **Charleston, SC** (8658120) - Charleston Harbor
- **Mayport, FL** (8720218) - St. Johns River
- **Miami Beach, FL** (8726520) - Atlantic Ocean
- **Panama City Beach, FL** (8729108) - Gulf of Mexico
- **Grand Isle, LA** (8761724) - Louisiana Coast
- **Sabine Pass North, TX** (8770570) - Texas-Louisiana Border
- **San Francisco, CA** (9414290) - San Francisco Bay
- **Seattle, WA** (9447130) - Puget Sound
- **Honolulu, HI** (1612340) - Pacific Ocean
- **San Juan, PR** (9751364) - Caribbean Sea

### USGS Water Services (Rivers/Streams)
- **Potomac River** at Little Falls, DC (01646500)
- **James River** at Richmond, VA (02035000)
- **Neuse River** at Kinston, NC (02089500)
- **Congaree River** at Columbia, SC (02169500)
- **Altamaha River** at Doctortown, GA (02228000)
- **Mississippi River** at Baton Rouge, LA (07374000)
- **Buffalo Bayou** at Houston, TX (08074000)
- **Colorado River** at Lee's Ferry, AZ (09380000)
- **Sacramento River** at Freeport, CA (11447650)
- **Cedar River** at Renton, WA (12113390)
- **Delaware River** at Trenton, NJ (01463500)
- **Jones Falls** at Baltimore, MD (01589000)
- **Scioto River** at Columbus, OH (03234500)

---

## 🚀 Quick Start

### 1. Enable Flood Monitoring

Edit `application.yaml`:

```yaml
app:
  scheduler:
    noaa-tides:
      enabled: true
      cron: "0 */5 * * * *"  # Every 5 minutes
    usgs-water:
      enabled: true
      cron: "0 */10 * * * *"  # Every 10 minutes
```

### 2. Run Data Collector

The flood monitoring will start automatically with the scheduled jobs.

### 3. Test Endpoints

```bash
# Check flood monitoring health
curl http://localhost:8082/api/v1/water-level/health

# Get all stations currently flooding
curl http://localhost:8082/api/v1/water-level/flooding

# Get latest water level for a specific station
curl http://localhost:8082/api/v1/water-level/station/8518750/latest

# Get water level history (last 24 hours)
curl http://localhost:8082/api/v1/water-level/station/8518750/history?hours=24

# Get monitoring statistics
curl http://localhost:8082/api/v1/water-level/stats

# Find stations near a location (NYC example)
curl "http://localhost:8082/api/v1/water-level/nearby?latitude=40.7128&longitude=-74.0060&radiusDegrees=1.0"

# Manual trigger collection
curl -X POST http://localhost:8082/api/v1/water-level/collect/all
```

---

## 🌊 Flood Severity Levels

The system detects flood conditions based on official National Weather Service (NWS) flood stages:

| Severity | Description | Action |
|----------|-------------|--------|
| **NORMAL** | Water level below action stage | Continue monitoring |
| **ACTION** | Reached action stage | Prepare for possible flooding |
| **MINOR** | Minor flooding beginning | Some roads may flood |
| **MODERATE** | Moderate flooding | Evacuations may be necessary |
| **MAJOR** | Major flooding | Serious threat to life and property |

---

## 📊 Kafka Topics

Flood data is published to Kafka for real-time processing:

### `raw.waterlevel.data`
All water level measurements (every 5-10 minutes)

```json
{
  "stationId": "8518750",
  "stationName": "The Battery, NY",
  "source": "noaa_tides",
  "locationType": "ocean",
  "latitude": 40.7006,
  "longitude": -74.0142,
  "timestamp": "2025-12-11T10:30:00Z",
  "waterLevelMeters": 1.234,
  "waterLevelFeet": 4.05,
  "datum": "MLLW",
  "floodSeverity": "NORMAL",
  "isFlooding": false,
  "qualityCode": "v"
}
```

### `raw.flood.alert`
Only when flooding is detected

```json
{
  "stationId": "01646500",
  "stationName": "Potomac River at Little Falls",
  "source": "usgs_water",
  "locationType": "river",
  "gageHeightFeet": 12.5,
  "floodStageFeet": 10.0,
  "floodSeverity": "MODERATE",
  "isFlooding": true,
  "timestamp": "2025-12-11T10:30:00Z"
}
```

---

## 🗄️ Database Schema

### `water_level_metrics` Table

```sql
CREATE TABLE water_level_metrics (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMPTZ NOT NULL,
    station_id VARCHAR(50) NOT NULL,
    station_name VARCHAR(255) NOT NULL,
    source VARCHAR(20) NOT NULL,
    location_type VARCHAR(20) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    water_level_meters DOUBLE PRECISION,
    water_level_feet DOUBLE PRECISION,
    datum VARCHAR(50),
    discharge_cfs DOUBLE PRECISION,
    gage_height_feet DOUBLE PRECISION,
    flood_stage_feet DOUBLE PRECISION,
    action_stage_feet DOUBLE PRECISION,
    minor_flood_stage_feet DOUBLE PRECISION,
    moderate_flood_stage_feet DOUBLE PRECISION,
    major_flood_stage_feet DOUBLE PRECISION,
    quality_code VARCHAR(10),
    raw_data JSONB,
    processed_at TIMESTAMPTZ
);

CREATE INDEX idx_station_timestamp ON water_level_metrics(station_id, timestamp);
CREATE INDEX idx_location_type ON water_level_metrics(location_type, timestamp);
CREATE INDEX idx_timestamp ON water_level_metrics(timestamp);
```

---

## 🔧 Configuration

### Circuit Breaker Settings

```yaml
resilience4j:
  circuitbreaker:
    instances:
      noaa-tides-api:
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
      usgs-water-api:
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
```

### API Endpoints

```yaml
app:
  noaa:
    tides:
      base-url: https://api.tidesandcurrents.noaa.gov/api/prod
  usgs:
    water:
      base-url: https://waterservices.usgs.gov/nwis/iv
```

---

## 🌍 Expanding Coverage

To add more monitoring stations:

### Add NOAA Station

Edit `NoaaTidesApiService.java`:

```java
private static final List<String> MONITORING_STATIONS = Arrays.asList(
    "8518750",  // Existing
    "YOUR_STATION_ID"  // Add new
);
```

Find station IDs: https://tidesandcurrents.noaa.gov/map/

### Add USGS Site

Edit `UsgsWaterApiService.java`:

```java
private static final List<String> MONITORING_SITES = Arrays.asList(
    "01646500",  // Existing
    "YOUR_SITE_CODE"  // Add new
);
```

Find site codes: https://waterdata.usgs.gov/nwis/rt

---

## 📈 Performance

- **NOAA API**: ~100ms response time, 5-minute collection interval
- **USGS API**: ~150ms response time, 10-minute collection interval
- **Database**: Indexed queries < 10ms
- **Kafka**: Async publishing, no blocking
- **Circuit Breaker**: Auto-recovery after 30s
- **Caching**: Redis cache for API responses

---

## 🔒 Data Quality

- **NOAA Quality Codes**: `v` (verified), `p` (preliminary)
- **USGS Qualifiers**: `P` (provisional), `A` (approved)
- **Filtering**: Only active stations with recent data
- **Validation**: Null checks, range validation
- **Fallback**: Returns cached data when API fails

---

## 🚨 Alert Integration

Connect to Alert Engine for advanced processing:

```java
@KafkaListener(topics = "raw.flood.alert")
public void handleFloodAlert(WaterLevelEvent event) {
    if ("MAJOR".equals(event.getFloodSeverity())) {
        // Send notifications
        // Trigger emergency protocols
        // Update dashboards
    }
}
```

---

## 📚 API Examples

### Get All Flooding Stations

```bash
curl http://localhost:8082/api/v1/water-level/flooding
```

Response:
```json
[
  {
    "stationId": "01646500",
    "stationName": "Potomac River at Little Falls",
    "waterLevelFeet": 12.5,
    "floodStageFeet": 10.0,
    "floodSeverity": "MODERATE",
    "timestamp": "2025-12-11T10:30:00Z"
  }
]
```

### Search Nearby Stations

```bash
curl "http://localhost:8082/api/v1/water-level/nearby?latitude=38.9072&longitude=-77.0369&radiusDegrees=0.5"
```

---

## 🎯 Future Enhancements

- [ ] Add precipitation/rainfall data integration
- [ ] Implement ML-based flood prediction
- [ ] Add storm surge modeling
- [ ] Integrate with weather forecast APIs
- [ ] Add SMS/email notification service
- [ ] Create real-time dashboard
- [ ] Add historical trend analysis
- [ ] Support for international stations

---

## 📞 Support

For questions or issues:
- Check logs: `data-collector/logs/application.log`
- Monitor health: `/actuator/health`
- View metrics: `/actuator/metrics`

---

**Now monitoring water levels and flood conditions across coastal and river systems in real-time! 🌊**
