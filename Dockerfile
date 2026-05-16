# =============================================================
# Multi-stage Dockerfile — resume-analyzer API
# Stage 1: Build with Maven
# Stage 2: Run with minimal JRE
# =============================================================

# ---- Stage 1: Build ----
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml first (layer caching)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies separately so this layer is cached
# unless pom.xml changes
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src/ src/

# Build, skip tests (tests run in CI before Docker build)
RUN ./mvnw package -DskipTests -B

# ---- Stage 2: Run ----
FROM eclipse-temurin:17-jre-alpine AS runtime

# Security: don't run as root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Create upload directory and set permissions
RUN mkdir -p /tmp/resumes && chown appuser:appgroup /tmp/resumes

# Copy only the built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set ownership
RUN chown appuser:appgroup app.jar

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# JVM tuning for containers
ENV JAVA_OPTS="\
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -XX:+ExitOnOutOfMemoryError \
  -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]