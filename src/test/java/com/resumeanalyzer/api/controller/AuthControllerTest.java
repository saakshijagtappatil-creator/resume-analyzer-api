package com.resumeanalyzer.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.api.config.SecurityConfig;
import com.resumeanalyzer.api.dto.request.LoginRequest;
import com.resumeanalyzer.api.dto.request.RegisterRequest;
import com.resumeanalyzer.api.dto.response.AuthResponse;
import com.resumeanalyzer.api.security.AuthEntryPoint;
import com.resumeanalyzer.api.security.CustomUserDetailsService;
import com.resumeanalyzer.api.security.JwtTokenProvider;
import com.resumeanalyzer.api.security.OAuth2AuthenticationSuccessHandler;
import com.resumeanalyzer.api.security.OAuth2UserService;
import com.resumeanalyzer.api.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, AuthEntryPoint.class})
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService authService;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean CustomUserDetailsService customUserDetailsService;
    @MockBean OAuth2UserService oAuth2UserService;
    @MockBean OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;

    @Test
    void register_returns201WithValidData() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("Password1");

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("jwt-token")
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .userId("user-1")
                .username("newuser")
                .email("new@example.com")
                .role("USER")
                .build();

        when(authService.register(any())).thenReturn(authResponse);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.data.username").value("newuser"));
    }

    @Test
    void register_returns400ForDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("duplicate@example.com");
        request.setPassword("Password1");

        when(authService.register(any()))
                .thenThrow(new IllegalArgumentException("Email already registered: duplicate@example.com"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already registered: duplicate@example.com"));
    }

    @Test
    void register_returns400ForWeakPassword() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("weakpassword");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    void login_returns200WithJwtToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmailOrUsername("test@example.com");
        request.setPassword("Password1");

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("jwt-token")
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .userId("user-1")
                .username("testuser")
                .email("test@example.com")
                .role("USER")
                .build();

        when(authService.login(any())).thenReturn(authResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void login_returns401ForWrongPassword() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmailOrUsername("test@example.com");
        request.setPassword("WrongPass1");

        when(authService.login(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
