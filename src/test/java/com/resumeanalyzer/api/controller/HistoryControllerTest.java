package com.resumeanalyzer.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.api.config.SecurityConfig;
import com.resumeanalyzer.api.dto.response.AnalysisResultResponse;
import com.resumeanalyzer.api.entity.User;
import com.resumeanalyzer.api.repository.UserRepository;
import com.resumeanalyzer.api.security.AuthEntryPoint;
import com.resumeanalyzer.api.security.CustomUserDetailsService;
import com.resumeanalyzer.api.security.JwtTokenProvider;
import com.resumeanalyzer.api.security.OAuth2AuthenticationSuccessHandler;
import com.resumeanalyzer.api.security.OAuth2UserService;
import com.resumeanalyzer.api.service.AnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HistoryController.class)
@Import({SecurityConfig.class, AuthEntryPoint.class})
@ActiveProfiles("test")
class HistoryControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AnalysisService analysisService;
    @MockBean UserRepository userRepository;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean CustomUserDetailsService customUserDetailsService;
    @MockBean OAuth2UserService oAuth2UserService;
    @MockBean OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-uuid-1")
                .username("testuser")
                .email("test@example.com")
                .role(User.Role.USER)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void getHistory_returnsListOfAnalysisResults() throws Exception {
        List<AnalysisResultResponse> history = List.of(
                AnalysisResultResponse.builder()
                        .id("result-1")
                        .resumeId("resume-1")
                        .originalFilename("resume.pdf")
                        .compatibilityScore(new BigDecimal("80.00"))
                        .extractedSkills(List.of("Java"))
                        .missingKeywords(List.of())
                        .suggestions(List.of())
                        .strengths(List.of())
                        .modelUsed("claude-opus-4-6")
                        .processingTimeMs(1000L)
                        .createdAt(Instant.now())
                        .build()
        );

        when(analysisService.getUserAnalysisHistory("user-uuid-1")).thenReturn(history);

        mockMvc.perform(get("/api/history")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value("result-1"))
                .andExpect(jsonPath("$.data[0].compatibilityScore").value(80.00));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void getStats_returnsCorrectCounts() throws Exception {
        HistoryController.HistoryStatsResponse stats =
                new HistoryController.HistoryStatsResponse(
                        5L,
                        new BigDecimal("78.40"),
                        new BigDecimal("95.00"),
                        new BigDecimal("60.00")
                );

        when(analysisService.getUserStats("user-uuid-1")).thenReturn(stats);

        mockMvc.perform(get("/api/history/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalResumes").value(5))
                .andExpect(jsonPath("$.data.averageScore").value(78.40))
                .andExpect(jsonPath("$.data.highestScore").value(95.00))
                .andExpect(jsonPath("$.data.lowestScore").value(60.00));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void getHistory_returnsEmptyListForNewUser() throws Exception {
        when(analysisService.getUserAnalysisHistory(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/history")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
