package com.resumeanalyzer.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.api.config.SecurityConfig;
import com.resumeanalyzer.api.dto.response.AnalysisResultResponse;
import com.resumeanalyzer.api.dto.response.ResumeStatusResponse;
import com.resumeanalyzer.api.entity.Resume;
import com.resumeanalyzer.api.entity.User;
import com.resumeanalyzer.api.repository.UserRepository;
import com.resumeanalyzer.api.security.AuthEntryPoint;
import com.resumeanalyzer.api.security.CustomUserDetailsService;
import com.resumeanalyzer.api.security.JwtTokenProvider;
import com.resumeanalyzer.api.security.OAuth2AuthenticationSuccessHandler;
import com.resumeanalyzer.api.security.OAuth2UserService;
import com.resumeanalyzer.api.service.AnalysisService;
import com.resumeanalyzer.api.service.ResumeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ResumeController.class)
@Import({SecurityConfig.class, AuthEntryPoint.class})
@ActiveProfiles("test")
class ResumeControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ResumeService resumeService;
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
    void upload_returns202WhenAuthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "pdf-content".getBytes());

        ResumeStatusResponse response = ResumeStatusResponse.builder()
                .id("resume-1")
                .originalFilename("resume.pdf")
                .fileSizeBytes(11L)
                .status(Resume.Status.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(resumeService.uploadResume(any(), any(), anyString())).thenReturn(response);

        mockMvc.perform(multipart("/api/resumes/upload").file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.originalFilename").value("resume.pdf"));
    }

    @Test
    void upload_returns401WhenNoToken() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "pdf-content".getBytes());

        mockMvc.perform(multipart("/api/resumes/upload").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void getStatus_returnsCorrectStatus() throws Exception {
        ResumeStatusResponse response = ResumeStatusResponse.builder()
                .id("resume-1")
                .originalFilename("resume.pdf")
                .fileSizeBytes(1000L)
                .status(Resume.Status.COMPLETED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(resumeService.getResumeStatus("resume-1", "user-uuid-1")).thenReturn(response);

        mockMvc.perform(get("/api/resumes/resume-1/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("resume-1"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void getResult_returnsAnalysisResult() throws Exception {
        AnalysisResultResponse response = AnalysisResultResponse.builder()
                .id("result-1")
                .resumeId("resume-1")
                .originalFilename("resume.pdf")
                .compatibilityScore(new BigDecimal("85.00"))
                .extractedSkills(List.of("Java", "Spring"))
                .missingKeywords(List.of("Docker"))
                .suggestions(List.of("Add cloud experience"))
                .strengths(List.of("Strong Java"))
                .modelUsed("claude-opus-4-6")
                .processingTimeMs(1200L)
                .createdAt(Instant.now())
                .build();

        when(analysisService.getAnalysisResult("resume-1", "user-uuid-1")).thenReturn(response);

        mockMvc.perform(get("/api/resumes/resume-1/result")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("result-1"))
                .andExpect(jsonPath("$.data.compatibilityScore").value(85.00))
                .andExpect(jsonPath("$.data.extractedSkills[0]").value("Java"));
    }
}
