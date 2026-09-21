package com.resumeanalyzer.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Circuit-breaker style recovery back-off for the listener container.
 * Failures are counted across all consumers; after {@code failureThreshold}
 * consecutive failures the breaker opens and the next retry is delayed by
 * {@code openDurationMs}. The count resets once a connection is established
 * (see {@link #reset()}).
 */
@Slf4j
public class RabbitCircuitBreakerBackOff implements BackOff {

    private final long initialIntervalMs;
    private final double multiplier;
    private final long maxIntervalMs;
    private final int failureThreshold;
    private final long openDurationMs;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();

    public RabbitCircuitBreakerBackOff(long initialIntervalMs, double multiplier, long maxIntervalMs,
                                       int failureThreshold, long openDurationMs) {
        this.initialIntervalMs = initialIntervalMs;
        this.multiplier = multiplier;
        this.maxIntervalMs = maxIntervalMs;
        this.failureThreshold = failureThreshold;
        this.openDurationMs = openDurationMs;
    }

    public void reset() {
        if (consecutiveFailures.getAndSet(0) > 0) {
            log.info("RabbitMQ circuit breaker closed: connection established");
        }
    }

    @Override
    public BackOffExecution start() {
        return () -> {
            int failures = consecutiveFailures.incrementAndGet();
            if (failures > failureThreshold) {
                // half-open: one trial attempt after the pause, then pause again if it fails
                consecutiveFailures.set(failureThreshold);
                log.warn("RabbitMQ circuit breaker OPEN after {} consecutive failures; pausing retries for {} s",
                        failureThreshold, openDurationMs / 1000);
                return openDurationMs;
            }
            long interval = (long) (initialIntervalMs * Math.pow(multiplier, failures - 1));
            return Math.min(interval, maxIntervalMs);
        };
    }
}
