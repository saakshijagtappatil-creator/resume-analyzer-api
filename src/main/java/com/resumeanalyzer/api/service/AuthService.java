package com.resumeanalyzer.api.service;

import com.resumeanalyzer.api.dto.request.LoginRequest;
import com.resumeanalyzer.api.dto.request.RegisterRequest;
import com.resumeanalyzer.api.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}