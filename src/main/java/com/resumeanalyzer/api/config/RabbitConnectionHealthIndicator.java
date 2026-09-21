package com.resumeanalyzer.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reports RabbitMQ connectivity without ever returning DOWN, so a broker outage
 * does not fail the platform health check and restart the app. Logs only on
 * state transitions to avoid flooding the logs.
 */
@Slf4j
@Component("rabbitConnection")
public class RabbitConnectionHealthIndicator implements HealthIndicator {

    private final ConnectionFactory connectionFactory;
    private final AtomicBoolean lastUp = new AtomicBoolean(true);

    public RabbitConnectionHealthIndicator(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Health health() {
        try {
            Connection connection = connectionFactory.createConnection();
            boolean open = connection.isOpen();
            if (open && !lastUp.getAndSet(true)) {
                log.info("RabbitMQ connection restored");
            }
            return open ? Health.up().build() : degraded("connection not open");
        } catch (Exception e) {
            return degraded(e.getMessage());
        }
    }

    private Health degraded(String reason) {
        if (lastUp.getAndSet(false)) {
            log.warn("RabbitMQ unavailable: {}", reason);
        }
        // UNKNOWN maps to HTTP 200, keeping the app in service while async work is paused
        return Health.unknown().withDetail("rabbitmq", "unavailable").withDetail("reason", String.valueOf(reason)).build();
    }
}
