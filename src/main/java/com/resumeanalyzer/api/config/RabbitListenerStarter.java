package com.resumeanalyzer.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Starts the RabbitMQ listener containers on a background thread once the application
 * is ready. Container start-up connects to the broker and can block, so it must not
 * run on the startup path.
 */
@Slf4j
@Component
public class RabbitListenerStarter {

    private final RabbitListenerEndpointRegistry registry;

    public RabbitListenerStarter(RabbitListenerEndpointRegistry registry) {
        this.registry = registry;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startListeners() {
        Thread starter = new Thread(() -> {
            try {
                registry.start();
                log.info("RabbitMQ listener containers started");
            } catch (Exception e) {
                log.warn("RabbitMQ listener start failed; container recovery will retry: {}", e.getMessage());
            }
        }, "rabbit-listener-starter");
        starter.setDaemon(true);
        starter.start();
    }
}
