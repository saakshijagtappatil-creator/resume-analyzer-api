package com.resumeanalyzer.api.config;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
public class GrafanaMetricsPusher {

    @Value("${app.grafana.push-url:}")
    private String pushUrl;

    @Value("${app.grafana.username:}")
    private String username;

    @Value("${app.grafana.password:}")
    private String password;

    @Autowired(required = false)
    private PrometheusMeterRegistry prometheusMeterRegistry;

    private final WebClient webClient;

    public GrafanaMetricsPusher(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    // Pushes metrics to Grafana Cloud every 60 seconds.
    // Targets the Mimir text-format push endpoint (/api/v1/push).
    // No-ops silently when GRAFANA_PUSH_URL is not configured.
    @Scheduled(fixedRateString = "${app.grafana.push-interval-ms:60000}")
    public void pushMetrics() {
        if (pushUrl.isBlank() || prometheusMeterRegistry == null) {
            return;
        }

        try {
            String metrics = prometheusMeterRegistry.scrape();
            String token = Base64.getEncoder().encodeToString(
                    (username + ":" + password).getBytes(StandardCharsets.UTF_8));

            webClient.post()
                    .uri(pushUrl)
                    .header("Authorization", "Basic " + token)
                    .header("Content-Type", "text/plain")
                    .bodyValue(metrics)
                    .retrieve()
                    .toBodilessEntity()
                    .subscribe(
                            resp -> log.debug("Metrics pushed to Grafana Cloud: {}", resp.getStatusCode()),
                            err  -> log.warn("Grafana Cloud metrics push failed: {}", err.getMessage())
                    );
        } catch (Exception e) {
            log.warn("Grafana Cloud metrics push error: {}", e.getMessage());
        }
    }
}
