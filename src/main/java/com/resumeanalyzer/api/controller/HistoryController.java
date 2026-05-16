package com.resumeanalyzer.api.controller;

import com.resumeanalyzer.api.dto.response.AnalysisResultResponse;
import com.resumeanalyzer.api.dto.response.ApiResponse;
import com.resumeanalyzer.api.entity.User;
import com.resumeanalyzer.api.repository.UserRepository;
import com.resumeanalyzer.api.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "History", description = "Analysis history endpoints")
public class HistoryController {

    private final AnalysisService analysisService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get all analysis results for the authenticated user")
    public ResponseEntity<ApiResponse<List<AnalysisResultResponse>>> getUserHistory(
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = getUserId(userDetails);

        log.info("Fetching analysis history for user: {}", userId);

        List<AnalysisResultResponse> history = analysisService
                .getUserAnalysisHistory(userId);

        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/filter")
    @Operation(summary = "Filter analysis history by minimum compatibility score")
    public ResponseEntity<ApiResponse<List<AnalysisResultResponse>>> filterByScore(
            @RequestParam(defaultValue = "0") BigDecimal minScore,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = getUserId(userDetails);

        log.info("Filtering history for user: {} with minScore: {}",
                userId, minScore);

        List<AnalysisResultResponse> history = analysisService
                .getFilteredAnalysisHistory(userId, minScore);

        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get analysis statistics for the authenticated user")
    public ResponseEntity<ApiResponse<HistoryStatsResponse>> getUserStats(
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = getUserId(userDetails);

        log.info("Fetching stats for user: {}", userId);

        HistoryStatsResponse stats = analysisService.getUserStats(userId);

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    private String getUserId(UserDetails userDetails) {
        User user = userRepository
                .findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + userDetails.getUsername()));
        return user.getId();
    }

    public record HistoryStatsResponse(
            long totalResumes,
            BigDecimal averageScore,
            BigDecimal highestScore,
            BigDecimal lowestScore
    ) {}
}