package com.resumeanalyzer.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Logs the RabbitMQ connection settings actually applied at runtime (never the password). */
@Slf4j
@Component
public class RabbitStartupLogger {

    private final ConnectionFactory connectionFactory;

    public RabbitStartupLogger(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logEffectiveConfig() {
        if (connectionFactory instanceof CachingConnectionFactory ccf) {
            log.info("RabbitMQ effective config: host={}, port={}, vhost={}, username={}, ssl={}",
                    ccf.getHost(), ccf.getPort(), ccf.getVirtualHost(), ccf.getUsername(),
                    ccf.getRabbitConnectionFactory().isSSL());
        } else {
            log.info("RabbitMQ connection factory: {}", connectionFactory.getClass().getName());
        }
    }
}
