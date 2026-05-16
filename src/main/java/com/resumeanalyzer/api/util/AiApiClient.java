package com.resumeanalyzer.api.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.api.entity.AnalysisResult;
import com.resumeanalyzer.api.entity.Resume;
import com.resumeanalyzer.api.exception.AIServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiApiClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final PdfTextExtractor pdfTextExtractor;

    @Value("${app.ai.api-key}")
    private String apiKey;

    @Value("${app.ai.model}")
    private String model;

    @Value("${app.ai.base-url}")
    private String baseUrl;

    @Value("${app.ai.max-tokens}")
    private int maxTokens;

    @Value("${app.ai.timeout-seconds}")
    private int timeoutSeconds;

    public AnalysisResult analyzeResume(Resume resume) {

        log.info("MOCK AI — analyzing resume: {}", resume.getId());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return AnalysisResult.builder()
                .resume(resume)
                .compatibilityScore(new BigDecimal("85.50"))
                .extractedSkills(List.of(
                        "Java",
                        "Spring Boot",
                        "PostgreSQL",
                        "Docker",
                        "REST APIs"))
                .missingKeywords(List.of(
                        "Kubernetes",
                        "AWS",
                        "Microservices"))
                .suggestions(List.of(
                        "Add quantifiable achievements to experience section",
                        "Include cloud platform experience",
                        "Add a professional summary at the top"))
                .strengths(List.of(
                        "Strong Java and Spring Boot background",
                        "Clear project descriptions",
                        "Relevant educational background"))
                .experienceSummary(
                        "Experienced Java developer with strong backend skills " +
                                "in Spring Boot and PostgreSQL. Shows good understanding " +
                                "of RESTful API design and database management.")
                .modelUsed("mock-model")
                .processingTimeMs(2000L)
                .build();
    }

    private String callClaudeApi(String prompt) {
        try {
            WebClient webClient = webClientBuilder
                    .baseUrl(baseUrl)
                    .defaultHeader("x-api-key", apiKey)
                    .defaultHeader("anthropic-version", "2023-06-01")
                    .defaultHeader("content-type", "application/json")
                    .build();

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "max_tokens", maxTokens,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            );

            String response = webClient.post()
                    .uri("/v1/messages")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            log.info("Claude API response received successfully");
            return response;

        } catch (WebClientResponseException e) {
            log.error("Claude API error: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new AIServiceException(
                    "Claude API returned error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Claude API call failed: {}", e.getMessage(), e);
            throw new AIServiceException("Claude API call failed", e);
        }
    }

    private String buildPrompt(String resumeText, String jobDescription) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
                You are an expert resume analyzer and career coach.
                Analyze the following resume and provide a detailed assessment.
                
                You MUST respond with valid JSON only. No other text before or after.
                
                Required JSON format:
                {
                  "compatibilityScore": <number 0-100>,
                  "extractedSkills": ["skill1", "skill2"],
                  "missingKeywords": ["keyword1", "keyword2"],
                  "suggestions": ["suggestion1", "suggestion2"],
                  "strengths": ["strength1", "strength2"],
                  "experienceSummary": "summary text"
                }
                
                """);

        prompt.append("RESUME:\n").append(resumeText).append("\n\n");

        if (jobDescription != null && !jobDescription.isBlank()) {
            prompt.append("""
                    JOB DESCRIPTION:
                    """).append(jobDescription).append("\n\n");
            prompt.append("""
                    Calculate the compatibility score based on how well the resume 
                    matches the job description. Include missing keywords from the 
                    job description that are not in the resume.
                    """);
        } else {
            prompt.append("""
                    No job description provided. Set compatibilityScore to null.
                    Focus on extracting skills, strengths, and general improvement 
                    suggestions for the resume.
                    """);
        }

        return prompt.toString();
    }

    private AnalysisResult parseAiResponse(String rawResponse, Resume resume) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);

            String content = root
                    .path("content")
                    .get(0)
                    .path("text")
                    .asText();

            JsonNode analysisJson = objectMapper.readTree(content);

            BigDecimal score = null;
            if (!analysisJson.path("compatibilityScore").isNull()) {
                score = analysisJson.path("compatibilityScore").decimalValue();
            }

            return AnalysisResult.builder()
                    .resume(resume)
                    .compatibilityScore(score)
                    .extractedSkills(parseStringList(
                            analysisJson.path("extractedSkills")))
                    .missingKeywords(parseStringList(
                            analysisJson.path("missingKeywords")))
                    .suggestions(parseStringList(
                            analysisJson.path("suggestions")))
                    .strengths(parseStringList(
                            analysisJson.path("strengths")))
                    .experienceSummary(
                            analysisJson.path("experienceSummary").asText())
                    .rawAiResponse(rawResponse)
                    .modelUsed(model)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse Claude AI response: {}", e.getMessage());
            throw new AIServiceException(
                    "Failed to parse AI response", e);
        }
    }

    private List<String> parseStringList(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> result.add(item.asText()));
        }
        return result;
    }
}