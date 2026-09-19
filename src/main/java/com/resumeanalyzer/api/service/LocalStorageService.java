package com.resumeanalyzer.api.service;

import com.resumeanalyzer.api.exception.InvalidFileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Filesystem-backed storage used when OCI is disabled (oci.enabled=false or unset).
 * Keeps the same "resumes/{id}" object-key format as {@link OciStorageService}.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "oci.enabled", havingValue = "false", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    @Override
    public String uploadFile(String resumeId, MultipartFile file) {
        String objectKey = "resumes/" + resumeId;
        try {
            Path target = resolve(objectKey);
            Files.createDirectories(target.getParent());
            file.transferTo(target);
            log.info("Saved resume {} to local storage: {}", resumeId, target);
            return objectKey;
        } catch (IOException e) {
            log.error("Local upload failed for resume: {}", resumeId, e);
            throw new InvalidFileException("Failed to save file locally: " + e.getMessage());
        }
    }

    @Override
    public byte[] downloadFile(String objectKey) {
        try {
            return Files.readAllBytes(resolve(objectKey));
        } catch (IOException e) {
            log.error("Local download failed for key: {}", objectKey, e);
            throw new InvalidFileException("Resume file is no longer available for download");
        }
    }

    @Override
    public void deleteFile(String objectKey) {
        try {
            Files.deleteIfExists(resolve(objectKey));
            log.info("Deleted local file: {}", objectKey);
        } catch (IOException e) {
            log.warn("Could not delete local file: {} — {}", objectKey, e.getMessage());
        }
    }

    private Path resolve(String objectKey) throws IOException {
        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path target = base.resolve(objectKey).normalize();
        if (!target.startsWith(base)) {
            throw new IOException("Invalid storage key");
        }
        return target;
    }
}
