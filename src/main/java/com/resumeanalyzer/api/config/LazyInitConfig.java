package com.resumeanalyzer.api.config;

import com.resumeanalyzer.api.messaging.consumer.ResumeAnalysisConsumer;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * With spring.main.lazy-initialization=true, beans that only react to events or
 * annotations would never be created. Keep those eager.
 */
@Configuration
public class LazyInitConfig {

    @Bean
    static LazyInitializationExcludeFilter eagerBeans() {
        return LazyInitializationExcludeFilter.forBeanTypes(
                ResumeAnalysisConsumer.class,       // @RabbitListener is registered when the bean is created
                RabbitAdmin.class,                  // declares queues/exchanges on connect
                RabbitListenerEndpointRegistry.class,
                RabbitListenerStarter.class,        // @EventListener
                RabbitStartupLogger.class,          // @EventListener
                MeterRegistry.class);               // keep metrics registries (incl. OTLP) active
    }
}
