ALTER TABLE stakeholders
    ADD COLUMN IF NOT EXISTS verification_date TIMESTAMP;

UPDATE stakeholders
SET verification_date = COALESCE(verification_date, CURRENT_TIMESTAMP)
WHERE COALESCE(applicant_fee_paid, FALSE) = TRUE
  AND verification_date IS NULL;
