ALTER TABLE stakeholders
    ADD COLUMN IF NOT EXISTS application_status VARCHAR(50) DEFAULT 'PENDING';

ALTER TABLE stakeholders
    ADD COLUMN IF NOT EXISTS treasurer_approved BOOLEAN DEFAULT FALSE;

ALTER TABLE stakeholders
    ADD COLUMN IF NOT EXISTS market_supervisor_approved BOOLEAN DEFAULT FALSE;

ALTER TABLE stakeholders
    ADD COLUMN IF NOT EXISTS bplo_approved BOOLEAN DEFAULT FALSE;

ALTER TABLE stakeholders
    ADD COLUMN IF NOT EXISTS final_endorsed BOOLEAN DEFAULT FALSE;

ALTER TABLE stakeholders
    ADD COLUMN IF NOT EXISTS applicant_fee_paid BOOLEAN DEFAULT FALSE;

UPDATE stakeholders
SET treasurer_approved = TRUE
WHERE COALESCE(treasurer_approved, FALSE) = FALSE
  AND (
      COALESCE(advance_payment_paid, FALSE) = TRUE
      OR COALESCE(advance_payment_completed, FALSE) = TRUE
  );

UPDATE stakeholders
SET final_endorsed = TRUE
WHERE COALESCE(final_endorsed, FALSE) = FALSE
  AND COALESCE(endorsing_approved, FALSE) = TRUE;

ALTER TABLE stakeholders
    ALTER COLUMN application_status SET DEFAULT 'PENDING',
    ALTER COLUMN treasurer_approved SET DEFAULT FALSE,
    ALTER COLUMN treasurer_approved SET NOT NULL,
    ALTER COLUMN final_endorsed SET DEFAULT FALSE,
    ALTER COLUMN final_endorsed SET NOT NULL;

ALTER TABLE stalls
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) DEFAULT 'AVAILABLE';

UPDATE stalls
SET status = CASE
    WHEN occupant_id IS NOT NULL THEN 'OCCUPIED'
    WHEN status IS NULL OR status = 'VACANT' THEN 'AVAILABLE'
    WHEN status IN ('AVAILABLE', 'OCCUPIED', 'MAINTENANCE') THEN status
    ELSE 'AVAILABLE'
END;

ALTER TABLE stalls
    ALTER COLUMN status SET DEFAULT 'AVAILABLE',
    ALTER COLUMN status SET NOT NULL;

CREATE TABLE IF NOT EXISTS approval_history (
    id BIGSERIAL PRIMARY KEY,
    stakeholder_id BIGINT NOT NULL REFERENCES stakeholders(id),
    stage VARCHAR(80),
    status VARCHAR(80),
    approved_by BIGINT REFERENCES users(id),
    remarks TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_approval_history_stakeholder
    ON approval_history(stakeholder_id);

CREATE INDEX IF NOT EXISTS idx_approval_history_stage
    ON approval_history(stage);
