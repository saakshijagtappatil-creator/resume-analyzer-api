package com.resumeanalyzer.api.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class CacheKeyGenerator {

    private static final String RESUME_ANALYSIS_PREFIX = "resume:analysis:";

    private CacheKeyGenerator() {
    }

    public static String forResume(String fileHash, String jobDescription) {
        String jobHash = (jobDescription != null && !jobDescription.isBlank())
                ? hashJobDescription(jobDescription)
                : "no-job";
        return RESUME_ANALYSIS_PREFIX + fileHash + ":" + jobHash;
    }

    private static String hashJobDescription(String jobDescription) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(
                    jobDescription.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}