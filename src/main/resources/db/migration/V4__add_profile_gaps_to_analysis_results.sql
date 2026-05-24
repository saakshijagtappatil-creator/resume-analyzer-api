ALTER TABLE analysis_results
    ADD COLUMN IF NOT EXISTS profile_gaps TEXT;
