# Multi-stage build for optimized Docker image

# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", \
  "app.jar"]
