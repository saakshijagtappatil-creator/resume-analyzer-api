package com.resumeanalyzer.api;

import com.rabbitmq.client.ConnectionFactory;
import com.resumeanalyzer.api.messaging.producer.ResumeAnalysisProducer;
import com.resumeanalyzer.api.service.OciStorageService;
import com.resumeanalyzer.api.util.AiApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ApiApplicationTests {

    @MockBean ConnectionFactory rabbitConnectionFactory;
    @MockBean RedisConnectionFactory redisConnectionFactory;
    @MockBean ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;
    @MockBean AiApiClient aiApiClient;
    @MockBean ResumeAnalysisProducer resumeAnalysisProducer;
    @MockBean OciStorageService ociStorageService;

    @Test
    void contextLoads() {
    }
}
