package com.resumeanalyzer.api.repository;

import com.resumeanalyzer.api.entity.Resume;
import com.resumeanalyzer.api.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ResumeRepositoryTest {

    @Autowired TestEntityManager entityManager;
    @Autowired ResumeRepository resumeRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("encoded-password")
                .role(User.Role.USER)
                .authProvider(User.AuthProvider.LOCAL)
                .enabled(true)
                .build();

        entityManager.persistAndFlush(testUser);
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_returnsResumesForUser() {
        Resume resume1 = Resume.builder()
                .user(testUser)
                .originalFilename("resume1.pdf")
                .storedPath("/tmp/resume1.pdf")
                .fileSizeBytes(1000L)
                .fileHash("hash1")
                .status(Resume.Status.COMPLETED)
                .build();

        Resume resume2 = Resume.builder()
                .user(testUser)
                .originalFilename("resume2.pdf")
                .storedPath("/tmp/resume2.pdf")
                .fileSizeBytes(2000L)
                .fileHash("hash2")
                .status(Resume.Status.PENDING)
                .build();

        entityManager.persistAndFlush(resume1);
        entityManager.persistAndFlush(resume2);

        List<Resume> resumes = resumeRepository
                .findByUserIdOrderByCreatedAtDesc(testUser.getId());

        assertThat(resumes).hasSize(2);
        assertThat(resumes).extracting(Resume::getUser)
                .allMatch(u -> u.getId().equals(testUser.getId()));
    }

    @Test
    void existsByFileHashAndUserIdAndJobDescriptionCustom_returnsTrueForDuplicate() {
        Resume resume = Resume.builder()
                .user(testUser)
                .originalFilename("resume.pdf")
                .storedPath("/tmp/resume.pdf")
                .fileSizeBytes(1000L)
                .fileHash("uniquehash")
                .status(Resume.Status.PENDING)
                .jobDescription(null)
                .build();

        entityManager.persistAndFlush(resume);

        boolean exists = resumeRepository
                .existsByFileHashAndUserIdAndJobDescriptionCustom(
                        "uniquehash", testUser.getId(), null);

        assertThat(exists).isTrue();
    }

    @Test
    void existsByFileHashAndUserIdAndJobDescriptionCustom_returnsFalseWhenNotFound() {
        boolean exists = resumeRepository
                .existsByFileHashAndUserIdAndJobDescriptionCustom(
                        "nonexistent-hash", testUser.getId(), null);

        assertThat(exists).isFalse();
    }
}