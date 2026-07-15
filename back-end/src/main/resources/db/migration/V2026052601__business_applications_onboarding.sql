CREATE TABLE IF NOT EXISTS business_applications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    business_name VARCHAR(120) NOT NULL,
    business_type VARCHAR(80) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    middle_name VARCHAR(255),
    last_name VARCHAR(80) NOT NULL,
    contact VARCHAR(40) NOT NULL,
    email VARCHAR(120),
    address TEXT NOT NULL,
    application_status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    onboarding_status VARCHAR(255) NOT NULL DEFAULT 'APPLICANT',
    application_form_paid BOOLEAN NOT NULL DEFAULT FALSE,
    advance_payment_completed BOOLEAN NOT NULL DEFAULT FALSE,
    advance_payment_amount NUMERIC(38, 2) NOT NULL DEFAULT 0,
    advance_payment_date DATE,
    advance_balance NUMERIC(38, 2) NOT NULL DEFAULT 0,
    total_advance_amount NUMERIC(38, 2) NOT NULL DEFAULT 0,
    market_approval_status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    endorsement_status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    endorsement_remarks TEXT,
    endorsed_at TIMESTAMP,
    bplo_status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    market_supervisor_approved BOOLEAN NOT NULL DEFAULT FALSE,
    bplo_approved BOOLEAN NOT NULL DEFAULT FALSE,
    endorsing_approved BOOLEAN NOT NULL DEFAULT FALSE,
    applicant_fee_paid BOOLEAN NOT NULL DEFAULT FALSE,
    applicant_fee_amount NUMERIC(38, 2) DEFAULT 0,
    applicant_fee_date DATE,
    verified_application BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,
    id_file_name VARCHAR(255),
    id_file_path VARCHAR(255),
    letter_file_name VARCHAR(255),
    letter_file_path VARCHAR(255),
    applied_on DATE,
    approved_on DATE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_business_application_user
    ON business_applications(user_id);

CREATE INDEX IF NOT EXISTS idx_business_application_status
    ON business_applications(application_status);

CREATE INDEX IF NOT EXISTS idx_business_application_applied_on
    ON business_applications(applied_on);

INSERT INTO business_applications (
    user_id,
    business_name,
    business_type,
    first_name,
    middle_name,
    last_name,
    contact,
    email,
    address,
    application_status,
    onboarding_status,
    application_form_paid,
    advance_payment_completed,
    advance_payment_amount,
    advance_payment_date,
    advance_balance,
    total_advance_amount,
    market_approval_status,
    endorsement_status,
    endorsement_remarks,
    endorsed_at,
    bplo_status,
    market_supervisor_approved,
    bplo_approved,
    endorsing_approved,
    applicant_fee_paid,
    applicant_fee_amount,
    applicant_fee_date,
    verified_application,
    notes,
    applied_on,
    approved_on,
    created_at,
    updated_at
)
SELECT
    s.user_id,
    s.business_name,
    s.business_type,
    s.first_name,
    s.middle_name,
    s.last_name,
    s.contact,
    s.email,
    s.address,
    COALESCE(s.application_status, 'PENDING'),
    COALESCE(s.onboarding_status, 'APPLICANT'),
    COALESCE(s.application_form_paid, FALSE),
    COALESCE(s.advance_payment_completed, FALSE),
    COALESCE(s.advance_payment_amount, 0),
    s.advance_payment_date,
    COALESCE(s.advance_balance, 0),
    COALESCE(s.total_advance_amount, 0),
    COALESCE(s.market_approval_status, 'PENDING'),
    COALESCE(s.endorsement_status, 'PENDING'),
    s.endorsement_remarks,
    s.endorsed_at,
    COALESCE(s.bplo_status, 'PENDING'),
    COALESCE(s.market_supervisor_approved, FALSE),
    COALESCE(s.bplo_approved, FALSE),
    COALESCE(s.endorsing_approved, FALSE),
    COALESCE(s.applicant_fee_paid, FALSE),
    COALESCE(s.applicant_fee_amount, 0),
    s.applicant_fee_date,
    COALESCE(s.verified_stakeholder, COALESCE(s.verified_tenant, FALSE)),
    s.notes,
    s.applied_on,
    s.approved_on,
    s.created_at,
    CURRENT_TIMESTAMP
FROM stakeholders s
WHERE s.user_id IS NOT NULL
ON CONFLICT (user_id) DO NOTHING;

ALTER TABLE stakeholders
    ADD COLUMN IF NOT EXISTS advance_payment_completed BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE stakeholders
SET advance_payment_completed = TRUE
WHERE COALESCE(advance_payment_completed, FALSE) = FALSE
  AND COALESCE(advance_balance, 0) >= COALESCE(total_advance_amount, 0)
  AND COALESCE(total_advance_amount, 0) > 0;
