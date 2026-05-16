package com.resumeanalyzer.api.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResumeUploadRequest {

    @Size(max = 5000, message = "Job description must not exceed 5000 characters")
    private String jobDescription;
}