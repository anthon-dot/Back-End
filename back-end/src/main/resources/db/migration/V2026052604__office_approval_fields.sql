ALTER TABLE business_applications
    ADD COLUMN IF NOT EXISTS endorsing_status VARCHAR(20) DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS final_status VARCHAR(20) DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS endorsed_by VARCHAR(100),
    ADD COLUMN IF NOT EXISTS bplo_approved_by VARCHAR(100),
    ADD COLUMN IF NOT EXISTS approval_date TIMESTAMP,
    ADD COLUMN IF NOT EXISTS remarks TEXT;

ALTER TABLE stakeholders
    ADD COLUMN IF NOT EXISTS endorsing_status VARCHAR(20) DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS final_status VARCHAR(20) DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS endorsed_by VARCHAR(100),
    ADD COLUMN IF NOT EXISTS bplo_approved_by VARCHAR(100),
    ADD COLUMN IF NOT EXISTS approval_date TIMESTAMP,
    ADD COLUMN IF NOT EXISTS remarks TEXT;

UPDATE business_applications
SET endorsing_status = CASE
        WHEN endorsement_status = 'APPROVED' THEN 'ENDORSED'
        WHEN endorsement_status = 'REJECTED' THEN 'REJECTED'
        ELSE COALESCE(endorsing_status, 'PENDING')
    END,
    final_status = CASE
        WHEN application_status = 'APPROVED' THEN 'APPROVED'
        WHEN application_status = 'REJECTED' THEN 'REJECTED'
        ELSE COALESCE(final_status, 'PENDING')
    END;

UPDATE stakeholders
SET endorsing_status = CASE
        WHEN endorsement_status = 'APPROVED' THEN 'ENDORSED'
        WHEN endorsement_status = 'REJECTED' THEN 'REJECTED'
        ELSE COALESCE(endorsing_status, 'PENDING')
    END,
    final_status = CASE
        WHEN application_status IN ('APPROVED', 'FULLY_APPROVED') THEN 'APPROVED'
        WHEN application_status = 'REJECTED' THEN 'REJECTED'
        ELSE COALESCE(final_status, 'PENDING')
    END;
