package com.resumeanalyzer.api.exception;

public class ResumeNotFoundException extends RuntimeException {

    public ResumeNotFoundException(String resumeId) {
        super("Resume not found with id: " + resumeId);
    }
}