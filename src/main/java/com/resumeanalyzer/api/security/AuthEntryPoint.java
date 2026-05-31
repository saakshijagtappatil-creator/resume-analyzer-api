package com.resumeanalyzer.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.api.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    private static final String[] API_PREFIXES = {
        "/api/", "/auth/", "/actuator/", "/swagger-ui", "/v3/api-docs"
    };

    // Known scanner/exploit paths — silently drop with 404, no log
    private static final String[] BOT_SCAN_SUFFIXES = {
        // Credential/config file scanners
        ".env", ".env.local", ".env.production", ".env.backup",
        "config.php", "setup.php", "install.php", "composer.json", "package.json",
        "client_secret", ".git/", ".svn/", ".DS_Store",
        // PHP/WordPress scanners
        "phpunit", "eval-stdin.php", ".php", "wp-login", "wp-admin",
        "xmlrpc.php",
        // API version/info probes (Kubernetes, Harbor, Portainer, etc.)
        "/version", "systeminfo", "/environment", "check-version",
        "hoverfly", "/cluster/", "cluster/summary",
        // Generic info/status probes not in our API surface
        "/v1/info", "/v2/info", "/v3/info",
        "/v1/about", "/v2/about", "/v3/about",
        "/v3/meta", "/system/status",
        // Spring Cloud Gateway / actuator scanner probes
        "gateway/routes", "gateway/globalfilters",
        // i18n/translation product probes
        "translation/products"
    };

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        String path = request.getRequestURI();

        boolean isApiPath = false;
        for (String prefix : API_PREFIXES) {
            if (path.startsWith(prefix)) {
                isApiPath = true;
                break;
            }
        }

        if (!isApiPath) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }

        String lowerPath = path.toLowerCase();
        for (String suffix : BOT_SCAN_SUFFIXES) {
            if (lowerPath.contains(suffix)) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                return;
            }
        }

        log.warn("Unauthorized request at {}: {}", path, authException.getMessage());

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message("You must be logged in to access this resource")
                .path(path)
                .timestamp(Instant.now())
                .build();

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                objectMapper.writeValueAsString(errorResponse)
        );
    }
}