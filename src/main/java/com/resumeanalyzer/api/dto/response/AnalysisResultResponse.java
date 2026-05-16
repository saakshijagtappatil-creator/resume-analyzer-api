package com.resumeanalyzer.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResultResponse {

    private String id;
    private String resumeId;
    private String originalFilename;
    private BigDecimal compatibilityScore;
    private List<String> extractedSkills;
    private List<String> missingKeywords;
    private List<String> suggestions;
    private List<String> strengths;
    private String experienceSummary;
    private String modelUsed;
    private Long processingTimeMs;
    private Instant createdAt;
}