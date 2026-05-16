package com.resumeanalyzer.api.dto.response;

import com.resumeanalyzer.api.entity.Resume.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeStatusResponse {

    private String id;
    private String originalFilename;
    private Long fileSizeBytes;
    private Status status;
    private String jobDescription;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;
}