package com.resumeanalyzer.api.service;

import com.resumeanalyzer.api.dto.request.ResumeUploadRequest;
import com.resumeanalyzer.api.dto.response.ResumeStatusResponse;
import com.resumeanalyzer.api.entity.Resume;
import com.resumeanalyzer.api.entity.User;
import com.resumeanalyzer.api.exception.InvalidFileException;
import com.resumeanalyzer.api.exception.ResumeNotFoundException;
import com.resumeanalyzer.api.messaging.producer.ResumeAnalysisProducer;
import com.resumeanalyzer.api.repository.ResumeRepository;
import com.resumeanalyzer.api.repository.UserRepository;
import com.resumeanalyzer.api.service.impl.ResumeServiceImpl;
import com.resumeanalyzer.api.util.PdfTextExtractor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock ResumeRepository resumeRepository;
    @Mock UserRepository userRepository;
    @Mock ResumeAnalysisProducer resumeAnalysisProducer;
    @Mock PdfTextExtractor pdfTextExtractor;
    @Mock OciStorageService ociStorageService;
    @InjectMocks ResumeServiceImpl resumeService;

    private static final String USER_ID = "user-uuid-1";
    private User testUser;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        ReflectionTestUtils.setField(resumeService, "maxSizeBytes", 5_242_880L);
        ReflectionTestUtils.setField(resumeService, "allowedContentType", "application/pdf");

        testUser = User.builder()
                .id(USER_ID)
                .username("testuser")
                .email("test@example.com")
                .role(User.Role.USER)
                .build();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void uploadResume_savesPendingResume() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "pdf-content".getBytes());
        ResumeUploadRequest request = new ResumeUploadRequest();

        Resume savedResume = Resume.builder()
                .id("resume-uuid-1")
                .user(testUser)
                .originalFilename("resume.pdf")
                .fileSizeBytes(file.getSize())
                .fileHash("hash123")
                .storedPath("resumes/resume-uuid-1")
                .status(Resume.Status.PENDING)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(pdfTextExtractor.generateFileHash(file)).thenReturn("hash123");
        when(resumeRepository.existsByFileHashAndUserIdAndJobDescriptionCustom(
                "hash123", USER_ID, null)).thenReturn(false);
        when(ociStorageService.uploadFile(any(), any())).thenReturn("resumes/resume-uuid-1");
        when(resumeRepository.save(any(Resume.class))).thenReturn(savedResume);

        ResumeStatusResponse response = resumeService.uploadResume(file, request, USER_ID);

        assertThat(response.getStatus()).isEqualTo(Resume.Status.PENDING);
        assertThat(response.getOriginalFilename()).isEqualTo("resume.pdf");
        verify(resumeRepository).save(any(Resume.class));
        verify(ociStorageService).uploadFile(any(), eq(file));
    }

    @Test
    void uploadResume_rejectsNonPdfFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.docx", "application/msword", "doc-content".getBytes());

        assertThatThrownBy(() ->
                resumeService.uploadResume(file, new ResumeUploadRequest(), USER_ID))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Only PDF files are allowed");

        verify(resumeRepository, never()).save(any());
    }

    @Test
    void uploadResume_rejectsOversizedFile() {
        byte[] bigContent = new byte[6_000_000];
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.pdf", "application/pdf", bigContent);

        assertThatThrownBy(() ->
                resumeService.uploadResume(file, new ResumeUploadRequest(), USER_ID))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("5MB");

        verify(resumeRepository, never()).save(any());
    }

    @Test
    void getResumeById_returnsResumeForCorrectUser() {
        Resume resume = Resume.builder()
                .id("resume-uuid-1")
                .user(testUser)
                .originalFilename("resume.pdf")
                .fileSizeBytes(1000L)
                .fileHash("hash123")
                .storedPath("resumes/resume-uuid-1")
                .status(Resume.Status.COMPLETED)
                .build();

        when(resumeRepository.findByIdAndUserId("resume-uuid-1", USER_ID))
                .thenReturn(Optional.of(resume));

        ResumeStatusResponse response = resumeService.getResumeStatus("resume-uuid-1", USER_ID);

        assertThat(response.getId()).isEqualTo("resume-uuid-1");
        assertThat(response.getStatus()).isEqualTo(Resume.Status.COMPLETED);
    }

    @Test
    void getResumeById_throwsForWrongUser() {
        when(resumeRepository.findByIdAndUserId("resume-uuid-1", "wrong-user"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                resumeService.getResumeStatus("resume-uuid-1", "wrong-user"))
                .isInstanceOf(ResumeNotFoundException.class);
    }
}
