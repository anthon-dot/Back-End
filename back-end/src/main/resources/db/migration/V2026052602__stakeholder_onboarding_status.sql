ALTER TABLE stakeholders
    ADD COLUMN IF NOT EXISTS onboarding_status VARCHAR(50);

ALTER TABLE stakeholders
    ALTER COLUMN onboarding_status TYPE VARCHAR(50);

ALTER TABLE stakeholders
    ADD COLUMN IF NOT EXISTS advance_payment_paid BOOLEAN DEFAULT FALSE;

ALTER TABLE stakeholders
    ADD COLUMN IF NOT EXISTS advance_payment_completed BOOLEAN DEFAULT FALSE;

UPDATE stakeholders
SET advance_payment_paid = TRUE
WHERE COALESCE(advance_payment_paid, FALSE) = FALSE
  AND (
      COALESCE(advance_payment_completed, FALSE) = TRUE
      OR (
          COALESCE(advance_balance, 0) >= COALESCE(total_advance_amount, 0)
          AND COALESCE(total_advance_amount, 0) > 0
      )
  );

UPDATE stakeholders
SET advance_payment_completed = TRUE
WHERE COALESCE(advance_payment_completed, FALSE) = FALSE
  AND COALESCE(advance_payment_paid, FALSE) = TRUE;

UPDATE stakeholders
SET onboarding_status = CASE
    WHEN application_status = 'APPROVED' THEN 'APPROVED'
    WHEN application_status = 'REJECTED' THEN 'REJECTED'
    WHEN COALESCE(advance_payment_paid, FALSE) = TRUE
      OR COALESCE(advance_payment_completed, FALSE) = TRUE THEN 'FOR_APPROVAL'
    WHEN onboarding_status IS NULL
      OR onboarding_status IN ('APPLICANT', 'BUSINESS_SUBMITTED') THEN 'PAYMENT_PENDING'
    WHEN onboarding_status IN (
        'ADVANCE_PAYMENT_COMPLETED',
        'MARKET_APPROVED',
        'STALL_ASSIGNED',
        'CONTRACT_CREATED',
        'BPLO_APPROVED',
        'ENDORSED',
        'TREASURER_PAID'
    ) THEN 'FOR_APPROVAL'
    WHEN onboarding_status IN ('VERIFIED_STAKEHOLDER', 'ACTIVE_OCCUPANT') THEN 'APPROVED'
    ELSE onboarding_status
END;

ALTER TABLE stakeholders
    ALTER COLUMN onboarding_status SET DEFAULT 'NEW',
    ALTER COLUMN onboarding_status SET NOT NULL;

ALTER TABLE stakeholders
    ALTER COLUMN advance_payment_paid SET DEFAULT FALSE,
    ALTER COLUMN advance_payment_paid SET NOT NULL;

ALTER TABLE stakeholders
    ALTER COLUMN advance_payment_completed SET DEFAULT FALSE,
    ALTER COLUMN advance_payment_completed SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_stakeholder_onboarding_status
    ON stakeholders(onboarding_status);

ALTER TABLE business_applications
    ADD COLUMN IF NOT EXISTS onboarding_status VARCHAR(50);

ALTER TABLE business_applications
    ALTER COLUMN onboarding_status TYPE VARCHAR(50);

UPDATE business_applications ba
SET onboarding_status = s.onboarding_status,
    application_status = s.application_status,
    advance_payment_completed = s.advance_payment_completed,
    advance_payment_amount = s.advance_payment_amount,
    advance_payment_date = s.advance_payment_date,
    advance_balance = s.advance_balance,
    total_advance_amount = s.total_advance_amount,
    verified_application = COALESCE(s.verified_stakeholder, s.verified_tenant, FALSE),
    approved_on = s.approved_on
FROM stakeholders s
WHERE ba.user_id = s.user_id;

UPDATE business_applications
SET onboarding_status = CASE
    WHEN application_status = 'APPROVED' THEN 'APPROVED'
    WHEN application_status = 'REJECTED' THEN 'REJECTED'
    WHEN COALESCE(advance_payment_completed, FALSE) = TRUE THEN 'FOR_APPROVAL'
    WHEN onboarding_status IS NULL
      OR onboarding_status IN ('APPLICANT', 'BUSINESS_SUBMITTED') THEN 'PAYMENT_PENDING'
    WHEN onboarding_status IN (
        'ADVANCE_PAYMENT_COMPLETED',
        'MARKET_APPROVED',
        'STALL_ASSIGNED',
        'CONTRACT_CREATED',
        'BPLO_APPROVED',
        'ENDORSED',
        'TREASURER_PAID'
    ) THEN 'FOR_APPROVAL'
    WHEN onboarding_status IN ('VERIFIED_STAKEHOLDER', 'ACTIVE_OCCUPANT') THEN 'APPROVED'
    ELSE onboarding_status
END;

ALTER TABLE business_applications
    ALTER COLUMN onboarding_status SET DEFAULT 'NEW',
    ALTER COLUMN onboarding_status SET NOT NULL;
