package com.resumeanalyzer.api.service;

import com.resumeanalyzer.api.controller.HistoryController;
import com.resumeanalyzer.api.dto.response.AnalysisResultResponse;

import java.math.BigDecimal;
import java.util.List;

public interface AnalysisService {

    AnalysisResultResponse getAnalysisResult(String resumeId, String userId);

    List<AnalysisResultResponse> getUserAnalysisHistory(String userId);

    void processResume(String resumeId);

    List<AnalysisResultResponse> getFilteredAnalysisHistory(
            String userId, BigDecimal minScore);

    HistoryController.HistoryStatsResponse getUserStats(String userId);
}