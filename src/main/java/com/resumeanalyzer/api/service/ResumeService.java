package com.resumeanalyzer.api.service;

import com.resumeanalyzer.api.dto.request.ResumeUploadRequest;
import com.resumeanalyzer.api.dto.response.ResumeStatusResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResumeService {

    ResumeStatusResponse uploadResume(
            MultipartFile file,
            ResumeUploadRequest request,
            String userId);

    ResumeStatusResponse getResumeStatus(String resumeId, String userId);

    List<ResumeStatusResponse> getUserResumes(String userId);

    void deleteResume(String resumeId, String userId);

    ResumeFileData downloadResume(String resumeId, String userId);

    record ResumeFileData(byte[] content, String filename) {}
}