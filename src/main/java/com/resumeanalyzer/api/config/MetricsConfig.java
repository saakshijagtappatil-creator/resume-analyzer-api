package com.resumeanalyzer.api.config;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.registry.otlp.OtlpConfig;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Configuration
public class MetricsConfig {

    @Value("${app.grafana.username:}")
    private String username;

    @Value("${app.grafana.password:}")
    private String password;

    @Value("${app.grafana.otlp-url:}")
    private String otlpUrl;

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
                .commonTags(
                        "application", "resume-analyzer",
                        "environment", "production"
                );
    }

    // Creates an OTLP registry that pushes to Grafana Cloud every 60 s.
    // Only active when app.grafana.otlp-url is set (i.e. in prod).
    @Bean
    @ConditionalOnProperty(name = "app.grafana.otlp-url")
    public OtlpMeterRegistry otlpMeterRegistry(Clock clock) {
        String encoded = Base64.getEncoder().encodeToString(
                (username + ":" + password).getBytes(StandardCharsets.UTF_8));

        OtlpConfig config = new OtlpConfig() {
            @Override
            public String get(String key) { return null; }

            @Override
            public String url() { return otlpUrl; }

            @Override
            public Duration step() { return Duration.ofSeconds(60); }

            @Override
            public Map<String, String> headers() {
                return Map.of("Authorization", "Basic " + encoded);
            }
        };

        return new OtlpMeterRegistry(config, clock);
    }
}
