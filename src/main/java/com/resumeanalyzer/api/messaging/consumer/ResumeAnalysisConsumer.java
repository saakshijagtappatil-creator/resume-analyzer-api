package com.resumeanalyzer.api.messaging.consumer;

import com.resumeanalyzer.api.config.RabbitMQConfig;
import com.resumeanalyzer.api.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeAnalysisConsumer {

    private final AnalysisService analysisService;

    @RabbitListener(
            queues = RabbitMQConfig.RESUME_ANALYSIS_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void consumeResumeAnalysis(String resumeId) {
        log.info("Received resume for analysis: {}", resumeId);

        try {
            analysisService.processResume(resumeId);
            log.info("Resume analysis completed: {}", resumeId);
        } catch (Exception e) {
            log.error("Failed to process resume: {}", resumeId, e);
            throw e;
        }
    }
}