package com.resumeanalyzer.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(new SecurityRequirement()
                        .addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token")));
    }

    private Info apiInfo() {
        return new Info()
                .title("Resume Analyzer API")
                .description("""
                        AI-powered Resume Analyzer and Job Match API.
                        
                        Features:
                        - Upload PDF resumes for AI analysis
                        - Match resumes against job descriptions
                        - Get compatibility scores and improvement suggestions
                        - JWT authentication and Google OAuth2 support
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Resume Analyzer Team")
                        .email("support@resumeanalyzer.com"))
                .license(new License()
                        .name("MIT License"));
    }
}