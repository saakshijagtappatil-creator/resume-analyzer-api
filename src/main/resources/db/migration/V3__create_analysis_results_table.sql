CREATE TABLE analysis_results (
    id VARCHAR(36) NOT NULL,
    resume_id VARCHAR(36) NOT NULL,
    compatibility_score NUMERIC(5,2),
    extracted_skills JSONB,
    missing_keywords JSONB,
    suggestions JSONB,
    strengths JSONB,
    experience_summary TEXT,
    raw_ai_response TEXT,
    model_used VARCHAR(100),
    processing_time_ms BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_analysis_results PRIMARY KEY (id),
    CONSTRAINT uk_analysis_resume UNIQUE (resume_id),
    CONSTRAINT fk_analysis_resume FOREIGN KEY (resume_id)
        REFERENCES resumes(id) ON DELETE CASCADE,
    CONSTRAINT chk_score_range CHECK (
        compatibility_score >= 0 AND compatibility_score <= 100
    )
);

CREATE INDEX idx_analysis_resume_id ON analysis_results(resume_id);
CREATE INDEX idx_analysis_score ON analysis_results(compatibility_score);