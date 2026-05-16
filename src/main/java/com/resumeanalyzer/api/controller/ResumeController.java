package com.resumeanalyzer.api.controller;

import com.resumeanalyzer.api.dto.request.ResumeUploadRequest;
import com.resumeanalyzer.api.dto.response.AnalysisResultResponse;
import com.resumeanalyzer.api.dto.response.ApiResponse;
import com.resumeanalyzer.api.dto.response.ResumeStatusResponse;
import com.resumeanalyzer.api.entity.User;
import com.resumeanalyzer.api.repository.UserRepository;
import com.resumeanalyzer.api.service.AnalysisService;
import com.resumeanalyzer.api.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Resume", description = "Resume upload and management endpoints")
public class ResumeController {

    private final ResumeService resumeService;
    private final AnalysisService analysisService;
    private final UserRepository userRepository;

    @PostMapping(value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a PDF resume for AI analysis")
    public ResponseEntity<ApiResponse<ResumeStatusResponse>> uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "jobDescription", required = false) String jobDescription,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = getUserId(userDetails);

        log.info("Resume upload request from user: {}", userId);

        ResumeUploadRequest request = new ResumeUploadRequest();
        request.setJobDescription(jobDescription);

        ResumeStatusResponse response = resumeService.uploadResume(
                file, request, userId);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(
                        response,
                        "Resume uploaded successfully and queued for analysis"));
    }

    @GetMapping("/{resumeId}/status")
    @Operation(summary = "Get resume processing status")
    public ResponseEntity<ApiResponse<ResumeStatusResponse>> getResumeStatus(
            @PathVariable String resumeId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = getUserId(userDetails);

        ResumeStatusResponse response = resumeService
                .getResumeStatus(resumeId, userId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{resumeId}/result")
    @Operation(summary = "Get AI analysis result for a resume")
    public ResponseEntity<ApiResponse<AnalysisResultResponse>> getAnalysisResult(
            @PathVariable String resumeId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = getUserId(userDetails);

        AnalysisResultResponse response = analysisService
                .getAnalysisResult(resumeId, userId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get all resumes for the authenticated user")
    public ResponseEntity<ApiResponse<List<ResumeStatusResponse>>> getUserResumes(
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = getUserId(userDetails);

        List<ResumeStatusResponse> resumes = resumeService
                .getUserResumes(userId);

        return ResponseEntity.ok(ApiResponse.success(resumes));
    }

    @DeleteMapping("/{resumeId}")
    @Operation(summary = "Delete a resume")
    public ResponseEntity<ApiResponse<Void>> deleteResume(
            @PathVariable String resumeId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userId = getUserId(userDetails);

        resumeService.deleteResume(resumeId, userId);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Resume deleted successfully"));
    }

    private String getUserId(UserDetails userDetails) {
        User user = userRepository
                .findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + userDetails.getUsername()));
        return user.getId();
    }
}