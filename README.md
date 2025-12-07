# Data Collector Service

## 📋 Tổng Quan

**Data Collector** là microservice thu thập dữ liệu thời tiết vũ trụ từ NASA và NOAA APIs, lưu trữ vào PostgreSQL và publish events lên Kafka để các service khác xử lý.

### Chức Năng Chính
- ✅ Thu thập **Kp-index** từ NOAA (mỗi 10 phút)
- ✅ Thu thập **CME (Coronal Mass Ejection)** từ NASA DONKI (mỗi 15 phút)
- ✅ Lưu trữ metrics vào PostgreSQL
- ✅ Publish events lên Kafka topics
- ✅ Circuit breaker & retry mechanism với Resilience4j
- ✅ Caching với Redis
- ✅ Service discovery với Eureka
- ✅ Monitoring với Prometheus & Grafana

---

## 🏗️ Kiến Trúc

```
┌─────────────────┐       ┌─────────────────┐
│   NASA DONKI    │       │   NOAA SWPC     │
│   CME API       │       │   Kp-index API  │
└────────┬────────┘       └────────┬────────┘
         │                         │
         │  HTTP                   │  HTTP
         │  (Circuit Breaker)      │  (Circuit Breaker)
         ▼                         ▼
    ┌────────────────────────────────┐
    │     Data Collector Service     │
    │  - NasaApiService              │
    │  - NoaaApiService              │
    │  - DataCollectorService        │
    │  - Scheduled Jobs              │
    └─┬──────────────────────────┬───┘
      │                          │
      │ Save                     │ Publish
      ▼                          ▼
┌─────────────┐           ┌──────────────┐
│ PostgreSQL  │           │    Kafka     │
│  metrics    │           │  - kp topic  │
│   table     │           │  - cme topic │
└─────────────┘           └──────────────┘
```

---

## 🚀 Quick Start

### Prerequisites
- **Java 17+**
- **Maven 3.9+**
- **Docker & Docker Compose**
- **PostgreSQL 15**
- **Apache Kafka**
- **Redis 7**

### 1. Clone & Build
```bash
cd data-collector
./mvnw clean package -DskipTests
```

### 2. Run with Docker Compose
```bash
# Đảm bảo Phase 1 infrastructure đang chạy
cd ../infra
docker-compose up -d

# Build Docker image
cd ../data-collector
docker build -t data-collector:latest .

# Run container
docker run -d \
  --name data-collector \
  -p 8082:8082 \
  --network ai-scientist-network \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/ \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/ai_scientist \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  data-collector:latest
```

### 3. Verify Service
```bash
# Health check
curl http://localhost:8082/actuator/health

# Manual trigger Kp-index collection
curl -X POST http://localhost:8082/api/v1/collector/collect/kp-index

# Manual trigger CME collection
curl -X POST http://localhost:8082/api/v1/collector/collect/cme

# Get recent metrics
curl "http://localhost:8082/api/v1/collector/metrics?source=noaa&hours=24"
```

---

## 🔧 Configuration

### Application Configuration (`application.yaml`)

```yaml
server:
  port: 8082

spring:
  application:
    name: data-collector
  
  datasource:
    url: jdbc:postgresql://localhost:5433/ai_scientist
    username: postgres
    password: postgres123
  
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

# NASA & NOAA API Configuration
app:
  nasa:
    api:
      donki-url: https://api.nasa.gov/DONKI/
      cme-endpoint: CME
      key: DEMO_KEY
  
  noaa:
    api:
      kp-index-url: https://services.swpc.noaa.gov/json/planetary_k_index_1m.json
  
  # Scheduler
  scheduler:
    nasa-cme:
      cron: "0 */15 * * * *"  # Every 15 minutes
      enabled: true
    noaa-kp:
      cron: "0 */10 * * * *"  # Every 10 minutes
      enabled: true
```

### Environment Variables
- `NASA_API_KEY`: NASA API key (default: DEMO_KEY)
- `SPRING_PROFILES_ACTIVE`: Active profile (dev/prod)
- `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`: Eureka server URL
- `SPRING_DATASOURCE_URL`: PostgreSQL connection URL
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers

---

## 📊 Database Schema

```sql
CREATE TABLE metrics (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    source VARCHAR(50) NOT NULL,
    metric_type VARCHAR(100) NOT NULL,
    kp_index DOUBLE PRECISION,
    cme_class VARCHAR(20),
    speed_kmh INTEGER,
    raw_data JSONB,
    processed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_metrics_timestamp ON metrics(timestamp);
CREATE INDEX idx_metrics_source ON metrics(source);
CREATE INDEX idx_metrics_type ON metrics(metric_type);
```

---

## 📡 API Endpoints

### Collector Endpoints

#### POST `/api/v1/collector/collect/kp-index`
Trigger manual Kp-index data collection

**Response:**
```json
"Kp index data collection triggered"
```

#### POST `/api/v1/collector/collect/cme`
Trigger manual CME data collection

**Response:**
```json
"CME data collection triggered"
```

#### GET `/api/v1/collector/metrics`
Get recent metrics

**Query Parameters:**
- `source` (default: noaa): Data source (noaa/nasa)
- `hours` (default: 24): Time range in hours

**Response:**
```json
[
  {
    "id": 1,
    "timestamp": "2024-12-07T10:00:00Z",
    "source": "noaa",
    "metricType": "kp_index",
    "kpIndex": 3.0,
    "processedAt": "2024-12-07T10:01:00Z"
  }
]
```

#### GET `/api/v1/collector/health`
Health check endpoint

---

## 🔍 Monitoring

### Prometheus Metrics
- **JVM metrics**: Memory, GC, threads
- **HTTP metrics**: Request rate, latency, errors
- **Database metrics**: Connection pool, query performance
- **Kafka metrics**: Producer throughput, errors
- **Custom metrics**: Data collection success rate

Access metrics at: `http://localhost:8082/actuator/prometheus`

### Grafana Dashboard
Import dashboard ID: `11378` (Spring Boot 2.1+ Statistics)

---

## 🧪 Testing

### Run Unit Tests
```bash
./mvnw test
```

### Run Integration Tests
```bash
./mvnw verify
```

### Test Coverage
```bash
./mvnw clean test jacoco:report
# Report: target/site/jacoco/index.html
```

---

## 🔐 Security Checklist

✅ **Non-root Docker user**  
✅ **No sensitive data in logs**  
✅ **API keys from environment variables**  
✅ **Input validation with Jakarta Bean Validation**  
✅ **SQL injection prevention with JPA**  
✅ **Circuit breaker for external APIs**  
✅ **Rate limiting (configured in API Gateway)**

---

## 📈 Performance Tuning

### JVM Options
```bash
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
```

### Resilience4j Configuration
- **Circuit Breaker**: 50% failure threshold, 10s wait duration
- **Retry**: 3 attempts, exponential backoff (2s, 4s, 8s)
- **Time Limiter**: 30s timeout for NASA/NOAA APIs

### Caching Strategy
- **Kp-index data**: 5 minutes TTL
- **CME data**: 10 minutes TTL
- **Redis eviction policy**: LRU

---

## 🐛 Troubleshooting

### Problem: Service không kết nối được Eureka
**Solution:**
```bash
# Kiểm tra Eureka health
curl http://localhost:8761/actuator/health

# Kiểm tra network
docker network inspect ai-scientist-network
```

### Problem: Kafka connection refused
**Solution:**
```bash
# Verify Kafka is running
docker ps | grep kafka

# Check Kafka logs
docker logs kafka
```

### Problem: NASA API rate limit exceeded
**Solution:**
- Đăng ký NASA API key tại: https://api.nasa.gov
- Update `NASA_API_KEY` environment variable
- Adjust scheduler cron expression

---

## 📦 Dependencies

| Dependency | Version | Purpose |
|-----------|---------|---------|
| Spring Boot | 3.2.5 | Framework |
| Spring Cloud | 2023.0.1 | Eureka, Config |
| Resilience4j | 2.1.0 | Circuit breaker |
| PostgreSQL | 15 | Database |
| Kafka | 3.6 | Event streaming |
| Redis | 7 | Caching |
| Testcontainers | 1.19.7 | Integration tests |

---

## 🤝 Contributing

Tham khảo [Backend Agent Checklist](../../.github/ai-agents/backend-agent.md) để đảm bảo code quality.

---

## 📄 License

MIT License - See [LICENSE](../meta/LICENSE) for details.

---

## 📞 Contact

- **Team**: AI Scientist Ecosystem
- **GitHub**: [ai-scientist-ecosystem](https://github.com/ai-scientist-ecosystem)
- **Issues**: [GitHub Issues](https://github.com/ai-scientist-ecosystem/data-collector/issues)
