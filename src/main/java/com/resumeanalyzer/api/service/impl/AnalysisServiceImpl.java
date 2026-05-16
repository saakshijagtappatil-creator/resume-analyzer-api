package com.resumeanalyzer.api.service.impl;

import com.resumeanalyzer.api.controller.HistoryController;
import com.resumeanalyzer.api.dto.response.AnalysisResultResponse;
import com.resumeanalyzer.api.entity.AnalysisResult;
import com.resumeanalyzer.api.entity.Resume;
import com.resumeanalyzer.api.exception.AIServiceException;
import com.resumeanalyzer.api.exception.ResumeNotFoundException;
import com.resumeanalyzer.api.repository.AnalysisResultRepository;
import com.resumeanalyzer.api.repository.ResumeRepository;
import com.resumeanalyzer.api.service.AnalysisService;
import com.resumeanalyzer.api.util.AiApiClient;
import com.resumeanalyzer.api.util.CacheKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final AnalysisResultRepository analysisResultRepository;
    private final ResumeRepository resumeRepository;
    private final AiApiClient aiApiClient;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional(readOnly = true)
    public AnalysisResultResponse getAnalysisResult(String resumeId, String userId) {
        log.info("Fetching analysis result for resume: {}", resumeId);

        AnalysisResult result = analysisResultRepository
                .findByResumeIdAndResumeUserId(resumeId, userId)
                .orElseThrow(() -> new ResumeNotFoundException(resumeId));

        return mapToResponse(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalysisResultResponse> getUserAnalysisHistory(String userId) {
        log.info("Fetching analysis history for user: {}", userId);

        return analysisResultRepository
                .findAllByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void processResume(String resumeId) {
        log.info("Starting AI analysis for resume: {}", resumeId);

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResumeNotFoundException(resumeId));

        resume.setStatus(Resume.Status.PROCESSING);
        resumeRepository.save(resume);

        String cacheKey = CacheKeyGenerator.forResume(
                resume.getFileHash(),
                resume.getJobDescription()
        );

        AnalysisResult cachedResult = (AnalysisResult) redisTemplate
                .opsForValue()
                .get(cacheKey);

        if (cachedResult != null) {
            log.info("Cache hit for resume hash: {}", resume.getFileHash());
            saveResultFromCache(resume, cachedResult);
            return;
        }

        try {
            long startTime = System.currentTimeMillis();

            AnalysisResult result = aiApiClient.analyzeResume(resume);

            long processingTime = System.currentTimeMillis() - startTime;
            result.setProcessingTimeMs(processingTime);
            result.setResume(resume);

            validateScore(result.getCompatibilityScore());

            AnalysisResult savedResult = analysisResultRepository.save(result);

            resume.setStatus(Resume.Status.COMPLETED);
            resumeRepository.save(resume);

            redisTemplate.opsForValue().set(
                    cacheKey,
                    savedResult,
                    Duration.ofHours(24)
            );

            log.info("Analysis completed for resume: {} in {}ms",
                    resumeId, processingTime);

        } catch (AIServiceException e) {
            log.error("AI analysis failed for resume: {}", resumeId, e);
            resume.setStatus(Resume.Status.FAILED);
            resume.setErrorMessage(e.getMessage());
            resumeRepository.save(resume);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during analysis for resume: {}", resumeId, e);
            resume.setStatus(Resume.Status.FAILED);
            resume.setErrorMessage("Unexpected error during analysis");
            resumeRepository.save(resume);
            throw new AIServiceException("Analysis failed unexpectedly", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalysisResultResponse> getFilteredAnalysisHistory(
            String userId, BigDecimal minScore) {

        return analysisResultRepository
                .findByUserIdAndMinScore(userId, minScore)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public HistoryController.HistoryStatsResponse getUserStats(String userId) {

        List<AnalysisResult> results = analysisResultRepository
                .findAllByUserId(userId);

        long total = results.size();

        BigDecimal average = analysisResultRepository
                .findAverageScoreByUserId(userId)
                .orElse(BigDecimal.ZERO);

        BigDecimal highest = results.stream()
                .filter(r -> r.getCompatibilityScore() != null)
                .map(AnalysisResult::getCompatibilityScore)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal lowest = results.stream()
                .filter(r -> r.getCompatibilityScore() != null)
                .map(AnalysisResult::getCompatibilityScore)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return new HistoryController.HistoryStatsResponse(
                total, average, highest, lowest);
    }

    private void saveResultFromCache(Resume resume, AnalysisResult cachedResult) {
        AnalysisResult newResult = AnalysisResult.builder()
                .resume(resume)
                .compatibilityScore(cachedResult.getCompatibilityScore())
                .extractedSkills(cachedResult.getExtractedSkills())
                .missingKeywords(cachedResult.getMissingKeywords())
                .suggestions(cachedResult.getSuggestions())
                .strengths(cachedResult.getStrengths())
                .experienceSummary(cachedResult.getExperienceSummary())
                .modelUsed(cachedResult.getModelUsed())
                .processingTimeMs(0L)
                .build();

        analysisResultRepository.save(newResult);

        resume.setStatus(Resume.Status.COMPLETED);
        resumeRepository.save(resume);

        log.info("Result saved from cache for resume: {}", resume.getId());
    }

    private void validateScore(BigDecimal score) {
        if (score == null) return;
        if (score.compareTo(BigDecimal.ZERO) < 0 ||
                score.compareTo(new BigDecimal("100.00")) > 0) {
            throw new AIServiceException(
                    "AI returned invalid score: " + score);
        }
    }

    private AnalysisResultResponse mapToResponse(AnalysisResult result) {
        return AnalysisResultResponse.builder()
                .id(result.getId())
                .resumeId(result.getResume().getId())
                .originalFilename(result.getResume().getOriginalFilename())
                .compatibilityScore(result.getCompatibilityScore())
                .extractedSkills(result.getExtractedSkills())
                .missingKeywords(result.getMissingKeywords())
                .suggestions(result.getSuggestions())
                .strengths(result.getStrengths())
                .experienceSummary(result.getExperienceSummary())
                .modelUsed(result.getModelUsed())
                .processingTimeMs(result.getProcessingTimeMs())
                .createdAt(result.getCreatedAt())
                .build();
    }
}