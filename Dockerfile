# ==============================================================================
# CivicConnect - Multi-Stage Production Dockerfile
# Optimized for Render, Railway, AWS ECS, Fly.io, and Cloud Run
# ==============================================================================

# Stage 1: Build the Application with Maven & JDK 17
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Pre-cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production executable JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal Production JRE Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create unprivileged user and pre-create data & uploads directories with full permissions
RUN addgroup -S civic && adduser -S civic -G civic && \
    mkdir -p /app/uploads /app/data && chown -R civic:civic /app

# Copy compiled JAR from builder stage
COPY --from=builder /build/target/civicconnect-1.0.0.jar app.jar

USER civic

# Production Environment Defaults
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

# Dynamic port binding from cloud provider environment variable ($PORT)
ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dserver.port=${PORT:-8080} -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
