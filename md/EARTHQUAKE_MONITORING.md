# 🌍 Earthquake Monitoring System

## Overview

The AI-Scientist-Ecosystem now includes **global earthquake monitoring and tsunami warning capabilities**. The system continuously tracks seismic activity worldwide, providing real-time alerts for dangerous earthquakes, catastrophic events, and potential tsunami threats.

### Data Source
- **USGS Earthquake API**: https://earthquake.usgs.gov/fdsnws/event/1/
- Coverage: **Global** (all continents and oceans)
- Update Frequency: **Every 2 minutes** (near real-time)
- Data Quality: Official government source (U.S. Geological Survey)

---

## 🚨 Severity Levels

Earthquakes are classified into 7 severity levels based on magnitude:

| Magnitude | Severity | Impact | Frequency/Year | Example |
|-----------|----------|--------|----------------|---------|
| < 3.0 | **MICRO** | Not felt, recorded by instruments only | Millions | Background seismic noise |
| 3.0 - 3.9 | **MINOR** | Often felt, rarely causes damage | ~100,000 | Small tremors |
| 4.0 - 4.9 | **LIGHT** | Felt by many, objects shake | ~10,000 | Noticeable shaking |
| 5.0 - 5.9 | **MODERATE** | Minor building damage possible | ~1,000 | 🚨 **DANGEROUS** threshold |
| 6.0 - 6.9 | **STRONG** | Significant structural damage | ~100 | Major regional impact |
| 7.0 - 7.9 | **MAJOR** | Serious damage over large areas | ~20 | 🔴 **CATASTROPHIC** threshold |
| 8.0+ | **GREAT** | Global catastrophe | ~1 | 2011 Tohoku (M9.1), 2004 Indian Ocean (M9.1) |

---

## 🌊 Tsunami Risk Assessment

The system calculates **tsunami risk scores (0-100)** based on three factors:

### Risk Factors:
1. **Magnitude >= 6.5**: Undersea megathrust earthquakes
   - M7.5+: +50 points
   - M6.5-7.4: +30 points

2. **Shallow Depth < 70km**: More energy reaches the ocean floor
   - < 30km: +25 points
   - 30-70km: +15 points

3. **USGS Tsunami Warning Flag**: Official alert issued
   - Warning active: +25 points

### Risk Levels:
- **0-29**: Low risk
- **30-49**: Moderate risk
- **50-69**: High risk 🚨
- **70-100**: Critical risk 🔴

---

## 📡 REST API Endpoints

Base URL: `http://localhost:8082/api/v1/earthquake`

### Health & Collection

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Service health check |
| POST | `/collect` | Trigger manual collection (last 24h, M >= 4.5) |
| POST | `/collect/significant` | Collect M >= 6.0 (last 7 days) |
| POST | `/collect/location?latitude={lat}&longitude={lon}&radiusDegrees={radius}&minMagnitude={mag}` | Collect earthquakes near location |

### Query Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/recent?hours={hours}` | Get earthquakes from last N hours (default: 24) |
| GET | `/latest` | Get most recent earthquake |
| GET | `/{earthquakeId}` | Get earthquake by USGS ID |
| GET | `/magnitude/{minMagnitude}` | Get earthquakes >= magnitude |
| GET | `/dangerous` | Get dangerous earthquakes (M >= 5.0) |
| GET | `/catastrophic` | Get catastrophic earthquakes (M >= 7.0) |
| GET | `/shallow` | Get shallow earthquakes (depth < 70km) |
| GET | `/region/{regionName}` | Get earthquakes in specific region |
| GET | `/alert/{alertLevel}` | Get earthquakes by USGS alert level (green/yellow/orange/red) |

### Geographic & Tsunami

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/location/nearby?latitude={lat}&longitude={lon}&radiusDegrees={radius}` | Get earthquakes within geographic area |
| GET | `/tsunami-warnings` | Get all earthquakes with tsunami warnings |
| GET | `/tsunami-risk/high` | Get earthquakes with tsunami risk score >= 50 |

### Statistics

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/stats` | Comprehensive statistics: total count, last 24h/7d, dangerous/catastrophic counts, tsunami warnings, latest earthquake |

---

## 💻 Usage Examples

### 1. Get Recent Earthquakes (Last 24 Hours)
```bash
curl http://localhost:8082/api/v1/earthquake/recent
```

### 2. Get Dangerous Earthquakes (M >= 5.0)
```bash
curl http://localhost:8082/api/v1/earthquake/dangerous
```

### 3. Get Catastrophic Earthquakes (M >= 7.0)
```bash
curl http://localhost:8082/api/v1/earthquake/catastrophic
```

### 4. Trigger Manual Collection
```bash
curl -X POST http://localhost:8082/api/v1/earthquake/collect
```

### 5. Get Earthquakes Near Tokyo
```bash
curl "http://localhost:8082/api/v1/earthquake/location/nearby?latitude=35.6762&longitude=139.6503&radiusDegrees=5.0"
```

### 6. Get Tsunami Warnings
```bash
curl http://localhost:8082/api/v1/earthquake/tsunami-warnings
```

### 7. Get Statistics
```bash
curl http://localhost:8082/api/v1/earthquake/stats
```

**Example Response:**
```json
{
  "totalEarthquakes": 1247,
  "last24Hours": 89,
  "last7Days": 532,
  "dangerous24h": 12,
  "catastrophic7days": 2,
  "tsunamiWarnings24h": 1,
  "timestamp": "2025-12-11T12:30:00Z",
  "latestEarthquake": {
    "earthquakeId": "us7000m123",
    "magnitude": 6.8,
    "location": "42 km E of Miyako, Japan",
    "eventTime": "2025-12-11T12:15:32Z",
    "severity": "STRONG"
  }
}
```

---

## 🗺️ Global Coverage - High-Risk Regions

### Pacific Ring of Fire 🔥
The most seismically active zone on Earth:

#### Asia-Pacific:
- 🇯🇵 **Japan**: ~1,500 earthquakes/year
  - Tokyo metropolitan area (35.6M people)
  - Fukushima, Kobe, Osaka
- 🇮🇩 **Indonesia**: Pacific & Indian Ocean plates
  - Java, Sumatra (2004 M9.1 tsunami)
- 🇵🇭 **Philippines**: Philippine Trench
- 🇹🇼 **Taiwan**: Eurasian & Philippine plates
- 🇳🇿 **New Zealand**: Alpine Fault, Christchurch

#### Americas:
- 🇺🇸 **California, USA**: San Andreas Fault
  - San Francisco, Los Angeles at risk
- 🇨🇱 **Chile**: Longest fault zone
  - 1960 Valdivia (M9.5 - largest ever recorded)
- 🇲🇽 **Mexico**: Cocos Plate subduction
- 🇵🇪 **Peru**: Nazca Plate
- 🇪🇨 **Ecuador**: Northern Andes

### Other High-Risk Zones:

#### Mediterranean & Middle East:
- 🇹🇷 **Turkey**: North Anatolian Fault
  - 2023 Kahramanmaraş (M7.8, 59,000+ deaths)
- 🇬🇷 **Greece**: Hellenic Arc
- 🇮🇹 **Italy**: African & Eurasian plates
- 🇮🇷 **Iran**: Zagros Mountains

#### Himalayas:
- 🇳🇵 **Nepal**: 2015 Gorkha (M7.8)
- 🇮🇳 **India**: Indo-Australian Plate

---

## ⚙️ Configuration

### Scheduled Collection
- **Frequency**: Every 2 minutes (configurable)
- **Minimum Magnitude**: 4.5 (configurable)
- **Historical Data**: Last 24 hours per collection

### application.yaml
```yaml
app:
  usgs:
    earthquake:
      base-url: https://earthquake.usgs.gov
      min-magnitude: 4.5
  
  scheduler:
    earthquake:
      cron: "0 */2 * * * *"  # Every 2 minutes
      enabled: true
```

---

## 📊 Kafka Event Streaming

### Topics:

1. **`raw.earthquake.data`**
   - All earthquake events (M >= 4.5)
   - Real-time data feed

2. **`raw.earthquake.alert`**
   - Dangerous earthquakes (M >= 5.0)
   - Significant earthquakes (M >= 6.0)
   - Requires immediate attention

3. **`raw.tsunami.warning`**
   - Tsunami warning flag from USGS
   - Tsunami risk score >= 50
   - Critical coastal alerts

### Event Structure:
```json
{
  "earthquakeId": "us7000m123",
  "magnitude": 6.8,
  "magnitudeType": "mw",
  "depthKm": 35.2,
  "latitude": 39.6942,
  "longitude": 143.0453,
  "eventTime": "2025-12-11T12:15:32Z",
  "location": "42 km E of Miyako, Japan",
  "region": "Japan",
  "severity": "STRONG",
  "dangerous": true,
  "catastrophic": false,
  "shallow": true,
  "tsunamiWarning": true,
  "tsunamiRiskScore": 75,
  "alertLevel": "orange",
  "significance": 748,
  "feltReports": 1240,
  "dataSource": "us",
  "eventUrl": "https://earthquake.usgs.gov/earthquakes/eventpage/us7000m123",
  "collectedAt": "2025-12-11T12:16:08Z",
  "eventType": "earthquake.alert"
}
```

---

## 🔧 Resilience & Reliability

### Circuit Breaker Configuration:
- **Sliding Window**: 10 requests
- **Failure Threshold**: 50%
- **Wait Duration**: 30 seconds
- **Half-Open State**: 3 test requests

### Retry Mechanism:
- **Max Attempts**: 3
- **Wait Duration**: 1 second
- **Exponential Backoff**: 2x multiplier

### Fallback Strategy:
- Returns cached earthquakes from database when API unavailable
- Last 24 hours of stored data

---

## 🏗️ Database Schema

### Table: `earthquake_metrics`

**Indexes:**
- `idx_earthquake_id` (UNIQUE): Fast lookup by USGS ID
- `idx_event_time`: Time-based queries
- `idx_magnitude`: Magnitude filtering
- `idx_tsunami_warning`: Tsunami alert queries

**Key Fields:**
- `earthquake_id`: Unique USGS identifier
- `magnitude`, `magnitude_type`: Richter/Moment magnitude
- `depth_km`: Hypocenter depth
- `latitude`, `longitude`: Epicenter location
- `event_time`: Earthquake occurrence timestamp
- `location`, `region`: Human-readable location
- `tsunami_warning`: USGS tsunami flag
- `alert_level`: green/yellow/orange/red
- `significance`: USGS impact score (0-1000+)

---

## 🎯 Use Cases

### 1. Emergency Response
- Real-time alerts for disaster response teams
- Automatic notification when M >= 6.0 earthquakes occur
- Tsunami evacuation triggers for coastal areas

### 2. Infrastructure Monitoring
- Track aftershocks after major earthquakes
- Assess damage risk to critical infrastructure
- Plan maintenance based on seismic activity

### 3. Scientific Research
- Historical earthquake pattern analysis
- Identify seismic clusters and fault activity
- Correlate with other geological events

### 4. Public Safety
- Early warning systems for populations
- Educational dashboards showing real-time activity
- Integration with mobile alert apps

---

## 📈 Integration with Other Systems

### Combined with Existing Features:
- **Flood Warning**: Earthquake → Tsunami → Coastal Flooding
- **Space Weather**: Solar flares can disrupt earthquake monitoring equipment
- **Alert Engine**: Unified multi-hazard alerting platform

---

## 🚀 Next Steps

### Planned Enhancements:
1. **Machine Learning**: Aftershock prediction
2. **Historical Analysis**: Pattern detection over decades
3. **Mobile Push Notifications**: Direct user alerts
4. **GIS Mapping**: Interactive earthquake visualization
5. **Multi-Source Integration**: 
   - European EMSC data
   - Japan Meteorological Agency
   - GeoNet (New Zealand)

---

## 📞 Support & Resources

- **USGS API Documentation**: https://earthquake.usgs.gov/fdsnws/event/1/
- **USGS Event Page**: https://earthquake.usgs.gov/earthquakes/map/
- **System Logs**: Check `data-collector` service logs
- **Monitoring**: Prometheus metrics at `/actuator/prometheus`

---

## ⚠️ Important Notes

1. **Minimum Magnitude**: System tracks M >= 4.5 by default (configurable)
2. **API Rate Limiting**: USGS has no official limits but be respectful
3. **Data Accuracy**: ~2-10 minute delay from actual earthquake occurrence
4. **Tsunami Warnings**: Always follow official local government alerts
5. **Database Growth**: Earthquake data accumulates quickly, plan for storage

---

**🌍 The AI-Scientist-Ecosystem is now monitoring Earth's seismic pulse 24/7!**
