-- ==============================================================================
-- Migration: Normalize Payment Categories & Update Entities
-- 1. Normalizes BUSINESS_PERMIT_PAYMENT to canonical APPLICATION_FEE via trigger
-- 2. Migrates existing payments rows from BUSINESS_PERMIT_PAYMENT to APPLICATION_FEE
-- 3. Updates business_applications table with missing applicant fee, verification, notes, docs, and dates
-- 4. Declares Java backend-only entity tables (users, archive_records) for compatibility
-- ==============================================================================

-- 1. Payment Type Normalization Trigger & Migration
CREATE OR REPLACE FUNCTION public.normalize_payment_type()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.payment_type::text = 'BUSINESS_PERMIT_PAYMENT' THEN
        NEW.payment_type := 'APPLICATION_FEE';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_normalize_payment_type ON public.payments;
CREATE TRIGGER trg_normalize_payment_type
    BEFORE INSERT OR UPDATE ON public.payments
    FOR EACH ROW EXECUTE FUNCTION public.normalize_payment_type();

-- Normalize existing payment rows with the legacy value to APPLICATION_FEE
UPDATE public.payments
SET payment_type = 'APPLICATION_FEE'
WHERE payment_type::text = 'BUSINESS_PERMIT_PAYMENT';

-- 2. Align business_applications columns
ALTER TABLE public.business_applications
    ADD COLUMN IF NOT EXISTS endorsing_approved BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS applicant_fee_paid BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS applicant_fee_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS applicant_fee_date DATE,
    ADD COLUMN IF NOT EXISTS verified_application BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS notes TEXT,
    ADD COLUMN IF NOT EXISTS id_file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS id_file_path TEXT,
    ADD COLUMN IF NOT EXISTS letter_file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS letter_file_path TEXT,
    ADD COLUMN IF NOT EXISTS approved_on DATE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

-- 3. Java Backend-Only Entity Mappings (users, archive_records)
CREATE TABLE IF NOT EXISTS public.users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    username VARCHAR(80) UNIQUE NOT NULL,
    password VARCHAR(120) NOT NULL,
    role VARCHAR(50),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    reset_token VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.archive_records (
    id BIGSERIAL PRIMARY KEY,
    entity_name VARCHAR(100),
    entity_id BIGINT,
    archived_by BIGINT REFERENCES public.users(id) ON DELETE SET NULL,
    reason TEXT,
    archived_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.archive_records ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Admins manage users table" ON public.users;
CREATE POLICY "Admins manage users table" ON public.users FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Admins manage archive_records" ON public.archive_records;
CREATE POLICY "Admins manage archive_records" ON public.archive_records FOR ALL USING (public.is_admin());
