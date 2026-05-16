package com.resumeanalyzer.api.util;

import com.resumeanalyzer.api.dto.response.AnalysisResultResponse;
import com.resumeanalyzer.api.dto.response.ResumeStatusResponse;
import com.resumeanalyzer.api.entity.AnalysisResult;
import com.resumeanalyzer.api.entity.Resume;
import org.springframework.stereotype.Component;

@Component
public class ResumeMapper {

    public ResumeStatusResponse toStatusResponse(Resume resume) {
        if (resume == null) return null;

        return ResumeStatusResponse.builder()
                .id(resume.getId())
                .originalFilename(resume.getOriginalFilename())
                .fileSizeBytes(resume.getFileSizeBytes())
                .status(resume.getStatus())
                .jobDescription(resume.getJobDescription())
                .errorMessage(resume.getErrorMessage())
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .build();
    }

    public AnalysisResultResponse toAnalysisResponse(AnalysisResult result) {
        if (result == null) return null;

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