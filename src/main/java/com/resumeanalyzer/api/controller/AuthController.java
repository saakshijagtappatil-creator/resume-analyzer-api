package com.resumeanalyzer.api.controller;

import com.resumeanalyzer.api.dto.request.LoginRequest;
import com.resumeanalyzer.api.dto.request.RegisterRequest;
import com.resumeanalyzer.api.dto.response.ApiResponse;
import com.resumeanalyzer.api.dto.response.AuthResponse;
import com.resumeanalyzer.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and login endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        log.info("Register request for email: {}", request.getEmail());

        AuthResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        response,
                        "User registered successfully"));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email/username and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("Login request for: {}", request.getEmailOrUsername());

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(response, "Login successful"));
    }
}