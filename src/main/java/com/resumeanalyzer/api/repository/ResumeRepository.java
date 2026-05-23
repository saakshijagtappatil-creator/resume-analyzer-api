package com.resumeanalyzer.api.repository;

import com.resumeanalyzer.api.entity.Resume;
import com.resumeanalyzer.api.entity.Resume.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, String> {

    List<Resume> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Resume> findByIdAndUserId(String id, String userId);

    List<Resume> findByStatus(Status status);

    Optional<Resume> findByFileHashAndUserId(String fileHash, String userId);

    @Query("SELECT COUNT(r) > 0 FROM Resume r WHERE r.fileHash = :fileHash " +
            "AND r.user.id = :userId " +
            "AND r.status <> com.resumeanalyzer.api.entity.Resume$Status.FAILED " +
            "AND ((:jobDescription IS NULL AND r.jobDescription IS NULL) " +
            "OR (:jobDescription IS NOT NULL AND r.jobDescription = :jobDescription))")
    boolean existsByFileHashAndUserIdAndJobDescriptionCustom(
            @Param("fileHash") String fileHash,
            @Param("userId") String userId,
            @Param("jobDescription") String jobDescription);
    long countByUserId(String userId);

    @Query("SELECT r FROM Resume r WHERE r.user.id = :userId AND r.status = :status ORDER BY r.createdAt DESC")
    List<Resume> findByUserIdAndStatus(@Param("userId") String userId, @Param("status") Status status);
}