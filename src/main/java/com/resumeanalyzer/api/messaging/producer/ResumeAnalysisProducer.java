package com.resumeanalyzer.api.messaging.producer;

import com.resumeanalyzer.api.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeAnalysisProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendResumeForAnalysis(String resumeId) {
        log.info("Sending resume for analysis: {}", resumeId);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.RESUME_EXCHANGE,
                RabbitMQConfig.RESUME_ROUTING_KEY,
                resumeId
        );

        log.info("Resume sent to queue successfully: {}", resumeId);
    }
}