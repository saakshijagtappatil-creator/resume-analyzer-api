package com.resumeanalyzer.api.service;

import com.resumeanalyzer.api.dto.request.LoginRequest;
import com.resumeanalyzer.api.dto.request.RegisterRequest;
import com.resumeanalyzer.api.dto.response.AuthResponse;
import com.resumeanalyzer.api.entity.User;
import com.resumeanalyzer.api.repository.UserRepository;
import com.resumeanalyzer.api.security.JwtTokenProvider;
import com.resumeanalyzer.api.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtTokenProvider jwtTokenProvider;
    @InjectMocks AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("Password1");

        loginRequest = new LoginRequest();
        loginRequest.setEmailOrUsername("test@example.com");
        loginRequest.setPassword("Password1");

        savedUser = User.builder()
                .id("user-uuid-1")
                .username("testuser")
                .email("test@example.com")
                .passwordHash("encoded-password")
                .role(User.Role.USER)
                .authProvider(User.AuthProvider.LOCAL)
                .enabled(true)
                .build();
    }

    @Test
    void register_createsUserSuccessfully() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateTokenFromUsername("testuser")).thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getRole()).isEqualTo("USER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throwsWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_throwsWhenUsernameAlreadyTaken() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsJwtTokenForValidCredentials() {
        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(jwtTokenProvider.generateToken(mockAuth)).thenReturn("jwt-token");
        when(userRepository.findByEmailOrUsername("test@example.com", "test@example.com"))
                .thenReturn(Optional.of(savedUser));
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void login_throwsOnInvalidPassword() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_throwsWhenUserNotFoundAfterAuthentication() {
        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(mockAuth);
        when(jwtTokenProvider.generateToken(mockAuth)).thenReturn("jwt-token");
        when(userRepository.findByEmailOrUsername(anyString(), anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }
}
