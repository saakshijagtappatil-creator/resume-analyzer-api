# ---- Stage 1: Build ----
FROM --platform=linux/amd64 maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src/ src/
RUN mvn package -DskipTests -B

# ---- Stage 2: Run ----
FROM --platform=linux/amd64 eclipse-temurin:17-jre AS runtime

WORKDIR /app

RUN groupadd -r appgroup && useradd -r -g appgroup appuser \
    && mkdir -p /tmp/resumes \
    && chown -R appuser:appgroup /app /tmp/resumes

COPY --from=builder /app/target/*.jar app.jar

USER appuser

EXPOSE 8080

ENV JAVA_OPTS="\
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -XX:+ExitOnOutOfMemoryError \
  -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]