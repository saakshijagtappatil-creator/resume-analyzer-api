package com.resumeanalyzer.api.repository;

import com.resumeanalyzer.api.entity.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, String> {

    Optional<AnalysisResult> findByResumeId(String resumeId);

    Optional<AnalysisResult> findByResumeIdAndResumeUserId(String resumeId, String userId);

    boolean existsByResumeId(String resumeId);

    @Query("SELECT a FROM AnalysisResult a WHERE a.resume.user.id = :userId ORDER BY a.createdAt DESC")
    List<AnalysisResult> findAllByUserId(@Param("userId") String userId);

    @Query("SELECT AVG(a.compatibilityScore) FROM AnalysisResult a WHERE a.resume.user.id = :userId")
    Optional<BigDecimal> findAverageScoreByUserId(@Param("userId") String userId);

    @Query("SELECT a FROM AnalysisResult a WHERE a.resume.user.id = :userId AND a.compatibilityScore >= :minScore ORDER BY a.compatibilityScore DESC")
    List<AnalysisResult> findByUserIdAndMinScore(
            @Param("userId") String userId,
            @Param("minScore") BigDecimal minScore);
}