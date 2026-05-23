package com.resumeanalyzer.api.service.impl;

import com.resumeanalyzer.api.dto.request.ResumeUploadRequest;
import com.resumeanalyzer.api.dto.response.ResumeStatusResponse;
import com.resumeanalyzer.api.entity.Resume;
import com.resumeanalyzer.api.entity.User;
import com.resumeanalyzer.api.exception.InvalidFileException;
import com.resumeanalyzer.api.exception.ResumeNotFoundException;
import com.resumeanalyzer.api.messaging.producer.ResumeAnalysisProducer;
import com.resumeanalyzer.api.repository.ResumeRepository;
import com.resumeanalyzer.api.repository.UserRepository;
import com.resumeanalyzer.api.service.ResumeService;
import com.resumeanalyzer.api.util.PdfTextExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final ResumeAnalysisProducer resumeAnalysisProducer;
    private final PdfTextExtractor pdfTextExtractor;

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    @Value("${app.file.max-size-bytes}")
    private long maxSizeBytes;

    @Value("${app.file.allowed-content-type}")
    private String allowedContentType;

    @Override
    @Transactional
    public ResumeStatusResponse uploadResume(
            MultipartFile file,
            ResumeUploadRequest request,
            String userId) {

        log.info("Processing resume upload for user: {}", userId);

        validateFile(file);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + userId));

        String fileHash = pdfTextExtractor.generateFileHash(file);

        if (resumeRepository.existsByFileHashAndUserIdAndJobDescriptionCustom(
                fileHash, userId, request.getJobDescription())) {
            log.info("Duplicate resume detected for user: {}", userId);
            return resumeRepository
                    .findByFileHashAndUserId(fileHash, userId)
                    .map(this::mapToStatusResponse)
                    .orElseThrow(() -> new ResumeNotFoundException(fileHash));
        }

        String storedPath = storeFile(file, userId);

        Resume resume = Resume.builder()
                .user(user)
                .originalFilename(file.getOriginalFilename())
                .storedPath(storedPath)
                .fileSizeBytes(file.getSize())
                .fileHash(fileHash)
                .jobDescription(request.getJobDescription())
                .status(Resume.Status.PENDING)
                .build();

        Resume savedResume = resumeRepository.save(resume);

        log.info("Resume saved with id: {}", savedResume.getId());

        String resumeId = savedResume.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                resumeAnalysisProducer.sendResumeForAnalysis(resumeId);
            }
        });

        return mapToStatusResponse(savedResume);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeStatusResponse getResumeStatus(String resumeId, String userId) {
        Resume resume = resumeRepository
                .findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResumeNotFoundException(resumeId));

        return mapToStatusResponse(resume);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeStatusResponse> getUserResumes(String userId) {
        return resumeRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToStatusResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteResume(String resumeId, String userId) {
        Resume resume = resumeRepository
                .findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResumeNotFoundException(resumeId));

        try {
            Files.deleteIfExists(Paths.get(resume.getStoredPath()));
            log.info("Deleted file: {}", resume.getStoredPath());
        } catch (IOException e) {
            log.warn("Could not delete file: {}", resume.getStoredPath());
        }

        resumeRepository.delete(resume);
        log.info("Resume deleted: {}", resumeId);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is empty or missing");
        }

        if (file.getSize() > maxSizeBytes) {
            throw new InvalidFileException(
                    "File size exceeds maximum allowed size of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals(allowedContentType)) {
            throw new InvalidFileException(
                    "Invalid file type. Only PDF files are allowed");
        }
    }

    private String storeFile(MultipartFile file, String userId) {
        try {
            Path uploadPath = Paths.get(uploadDir, userId);
            Files.createDirectories(uploadPath);

            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(filename);

            Files.copy(file.getInputStream(), filePath,
                    StandardCopyOption.REPLACE_EXISTING);

            log.info("File stored at: {}", filePath);
            return filePath.toString();

        } catch (IOException e) {
            throw new InvalidFileException(
                    "Could not store file: " + e.getMessage());
        }
    }

    private ResumeStatusResponse mapToStatusResponse(Resume resume) {
        return ResumeStatusResponse.builder()
                .id(resume.getId())
                .originalFilename(resume.getOriginalFilename())
                .fileSizeBytes(resume.getFileSizeBytes())
                .status(resume.getStatus())
                .jobDescription(resume.getJobDescription())
                .errorMessage(resume.getErrorMessage())
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .build();
    }
}