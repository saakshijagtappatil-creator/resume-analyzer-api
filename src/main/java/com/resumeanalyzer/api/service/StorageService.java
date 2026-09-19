package com.resumeanalyzer.api.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String uploadFile(String resumeId, MultipartFile file);

    byte[] downloadFile(String objectKey);

    void deleteFile(String objectKey);
}
