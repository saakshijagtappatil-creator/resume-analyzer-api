package com.resumeanalyzer.api.service;

import com.resumeanalyzer.api.dto.response.AnalysisResultResponse;
import com.resumeanalyzer.api.entity.AnalysisResult;
import com.resumeanalyzer.api.entity.Resume;
import com.resumeanalyzer.api.entity.User;
import com.resumeanalyzer.api.exception.AIServiceException;
import com.resumeanalyzer.api.exception.ResumeNotFoundException;
import com.resumeanalyzer.api.repository.AnalysisResultRepository;
import com.resumeanalyzer.api.repository.ResumeRepository;
import com.resumeanalyzer.api.service.impl.AnalysisServiceImpl;
import com.resumeanalyzer.api.util.AiApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock AnalysisResultRepository analysisResultRepository;
    @Mock ResumeRepository resumeRepository;
    @Mock AiApiClient aiApiClient;
    @SuppressWarnings("rawtypes") @Mock RedisTemplate redisTemplate;
    @SuppressWarnings("rawtypes") @Mock ValueOperations valueOperations;
    @InjectMocks AnalysisServiceImpl analysisService;

    private Resume testResume;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-uuid-1")
                .username("testuser")
                .email("test@example.com")
                .build();

        testResume = Resume.builder()
                .id("resume-uuid-1")
                .user(testUser)
                .originalFilename("resume.pdf")
                .fileSizeBytes(1000L)
                .fileHash("abc123hash")
                .storedPath("/tmp/resume.pdf")
                .status(Resume.Status.PENDING)
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void processResume_setsCompletedStatusAfterSuccessfulAnalysis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AnalysisResult aiResult = AnalysisResult.builder()
                .compatibilityScore(new BigDecimal("85.00"))
                .extractedSkills(List.of("Java", "Spring Boot"))
                .missingKeywords(List.of("Docker"))
                .suggestions(List.of("Add cloud experience"))
                .strengths(List.of("Strong OOP skills"))
                .modelUsed("claude-opus-4-6")
                .build();

        when(resumeRepository.findById("resume-uuid-1")).thenReturn(Optional.of(testResume));
        when(valueOperations.get(anyString())).thenReturn(null);
        when(aiApiClient.analyzeResume(testResume)).thenReturn(aiResult);
        when(analysisResultRepository.save(any(AnalysisResult.class))).thenReturn(aiResult);
        when(resumeRepository.save(any(Resume.class))).thenReturn(testResume);

        analysisService.processResume("resume-uuid-1");

        verify(resumeRepository, times(2)).save(any(Resume.class));
        verify(analysisResultRepository).save(any(AnalysisResult.class));
        verify(valueOperations).set(anyString(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void processResume_setsFailedStatusOnAiError() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(resumeRepository.findById("resume-uuid-1")).thenReturn(Optional.of(testResume));
        when(valueOperations.get(anyString())).thenReturn(null);
        when(aiApiClient.analyzeResume(testResume))
                .thenThrow(new AIServiceException("AI API unavailable"));
        when(resumeRepository.save(any(Resume.class))).thenReturn(testResume);

        assertThatThrownBy(() -> analysisService.processResume("resume-uuid-1"))
                .isInstanceOf(AIServiceException.class);

        verify(resumeRepository, atLeastOnce()).save(argThat(r ->
                r.getStatus() == Resume.Status.FAILED));
        verify(analysisResultRepository, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void processResume_returnsCachedResultWithoutCallingAi() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AnalysisResult cachedResult = AnalysisResult.builder()
                .compatibilityScore(new BigDecimal("75.00"))
                .extractedSkills(List.of("Python"))
                .missingKeywords(List.of())
                .suggestions(List.of())
                .strengths(List.of())
                .modelUsed("claude-opus-4-6")
                .build();

        when(resumeRepository.findById("resume-uuid-1")).thenReturn(Optional.of(testResume));
        when(valueOperations.get(anyString())).thenReturn(cachedResult);
        when(resumeRepository.save(any(Resume.class))).thenReturn(testResume);
        when(analysisResultRepository.save(any(AnalysisResult.class))).thenReturn(cachedResult);

        analysisService.processResume("resume-uuid-1");

        verify(aiApiClient, never()).analyzeResume(any());
        verify(analysisResultRepository).save(any(AnalysisResult.class));
        verify(valueOperations, never()).set(anyString(), any(), any());
    }

    @Test
    void getAnalysisResult_returnsResultForUser() {
        AnalysisResult result = AnalysisResult.builder()
                .id("result-uuid-1")
                .resume(testResume)
                .compatibilityScore(new BigDecimal("90.00"))
                .extractedSkills(List.of("Java"))
                .missingKeywords(List.of())
                .suggestions(List.of())
                .strengths(List.of())
                .modelUsed("claude-opus-4-6")
                .processingTimeMs(1500L)
                .build();

        when(analysisResultRepository.findByResumeIdAndResumeUserId("resume-uuid-1", "user-uuid-1"))
                .thenReturn(Optional.of(result));

        AnalysisResultResponse response =
                analysisService.getAnalysisResult("resume-uuid-1", "user-uuid-1");

        assertThat(response.getCompatibilityScore()).isEqualByComparingTo(new BigDecimal("90.00"));
        assertThat(response.getResumeId()).isEqualTo("resume-uuid-1");
        assertThat(response.getProcessingTimeMs()).isEqualTo(1500L);
    }

    @Test
    void getAnalysisResult_throwsWhenResultNotFound() {
        when(analysisResultRepository.findByResumeIdAndResumeUserId(anyString(), anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                analysisService.getAnalysisResult("resume-uuid-1", "user-uuid-1"))
                .isInstanceOf(ResumeNotFoundException.class);
    }
}
