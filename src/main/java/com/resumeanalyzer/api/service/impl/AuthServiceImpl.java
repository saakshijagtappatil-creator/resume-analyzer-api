package com.resumeanalyzer.api.service.impl;

import com.resumeanalyzer.api.dto.request.LoginRequest;
import com.resumeanalyzer.api.dto.request.RegisterRequest;
import com.resumeanalyzer.api.dto.response.AuthResponse;
import com.resumeanalyzer.api.entity.User;
import com.resumeanalyzer.api.repository.UserRepository;
import com.resumeanalyzer.api.security.JwtTokenProvider;
import com.resumeanalyzer.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        log.info("Registering new user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Email already registered: " + request.getEmail());
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException(
                    "Username already taken: " + request.getUsername());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .authProvider(User.AuthProvider.LOCAL)
                .role(User.Role.USER)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        log.info("User registered successfully: {}", savedUser.getId());

        String token = jwtTokenProvider.generateTokenFromUsername(savedUser.getUsername());

        return buildAuthResponse(savedUser, token);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        log.info("Login attempt for: {}", request.getEmailOrUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmailOrUsername(),
                        request.getPassword()
                )
        );

        String token = jwtTokenProvider.generateToken(authentication);

        User user = userRepository
                .findByEmailOrUsername(
                        request.getEmailOrUsername(),
                        request.getEmailOrUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        log.info("Login successful for user: {}", user.getId());

        return buildAuthResponse(user, token);
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}