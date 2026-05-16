package com.resumeanalyzer.api.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Queue names
    public static final String RESUME_ANALYSIS_QUEUE = "resume.analysis.queue";
    public static final String RESUME_ANALYSIS_DLQ = "resume.analysis.dlq";

    // Exchange names
    public static final String RESUME_EXCHANGE = "resume.exchange";
    public static final String RESUME_DLX = "resume.dlx";

    // Routing keys
    public static final String RESUME_ROUTING_KEY = "resume.analysis";
    public static final String RESUME_DLQ_ROUTING_KEY = "resume.analysis.dead";

    // ─── Exchanges ────────────────────────────────────────────

    @Bean
    public DirectExchange resumeExchange() {
        return new DirectExchange(RESUME_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(RESUME_DLX, true, false);
    }

    // ─── Queues ───────────────────────────────────────────────

    @Bean
    public Queue resumeAnalysisQueue() {
        return QueueBuilder
                .durable(RESUME_ANALYSIS_QUEUE)
                .withArgument("x-dead-letter-exchange", RESUME_DLX)
                .withArgument("x-dead-letter-routing-key", RESUME_DLQ_ROUTING_KEY)
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder
                .durable(RESUME_ANALYSIS_DLQ)
                .build();
    }

    // ─── Bindings ─────────────────────────────────────────────

    @Bean
    public Binding resumeAnalysisBinding() {
        return BindingBuilder
                .bind(resumeAnalysisQueue())
                .to(resumeExchange())
                .with(RESUME_ROUTING_KEY);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(RESUME_DLQ_ROUTING_KEY);
    }

    // ─── Message Converter ────────────────────────────────────

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ─── RabbitTemplate ──────────────────────────────────────

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    // ─── Listener Factory ─────────────────────────────────────

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(5);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}