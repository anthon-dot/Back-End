-- ==============================================================================
-- SUPABASE DATABASE SCHEMA FOR RENTAL MANAGEMENT SYSTEM
-- Fully replaces Spring Boot JPA/Hibernate entities and database schema
-- ==============================================================================

-- 1. Enable required PostgreSQL extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==============================================================================
-- 2. CUSTOM TYPES / ENUMS
-- ==============================================================================
DO $$ BEGIN
    CREATE TYPE payment_type_enum AS ENUM (
        'ADVANCE_PAYMENT',
        'BUSINESS_PERMIT_PAYMENT',
        'APPLICATION_FEE',
        'APPLICATION_FORM',
        'RENT_PAYMENT'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- ==============================================================================
-- 3. PROFILES / USERS (Synchronized with Supabase Auth)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username VARCHAR(80) UNIQUE,
    name VARCHAR(255),
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Auto-sync new auth.users to public.profiles
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, username, name, role, status)
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'username', NEW.email),
        COALESCE(NEW.raw_user_meta_data->>'name', ''),
        COALESCE(NEW.raw_user_meta_data->>'role', 'USER'),
        'ACTIVE'
    )
    ON CONFLICT (id) DO UPDATE SET
        username = EXCLUDED.username,
        name = EXCLUDED.name;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Helper function to check if current authenticated user is an ADMIN
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM public.profiles
        WHERE id = auth.uid() AND UPPER(role) = 'ADMIN'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

-- ==============================================================================
-- 4. STALL TYPES & STALLS
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.stall_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.stalls (
    id BIGSERIAL PRIMARY KEY,
    stall_no VARCHAR(100) NOT NULL UNIQUE,
    stall_type VARCHAR(100) NOT NULL,
    monthly_rent NUMERIC(12, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    image_url TEXT,
    info VARCHAR(500),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    occupant_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_stalls_status ON public.stalls(status);
CREATE INDEX IF NOT EXISTS idx_stalls_type ON public.stalls(stall_type);

-- ==============================================================================
-- 5. RENTAL RATES
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.rental_rates (
    id BIGSERIAL PRIMARY KEY,
    stall_type VARCHAR(100) NOT NULL,
    monthly_rate NUMERIC(12, 2) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ==============================================================================
-- 6. STAKEHOLDERS
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.stakeholders (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    business_name VARCHAR(120) NOT NULL,
    business_type VARCHAR(80) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    middle_name VARCHAR(80),
    last_name VARCHAR(80) NOT NULL,
    contact VARCHAR(40) NOT NULL,
    email VARCHAR(120),
    address TEXT NOT NULL,
    selected_stall_id BIGINT REFERENCES public.stalls(id) ON DELETE SET NULL,

    -- Application & Onboarding
    application_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    onboarding_status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    notes TEXT,

    -- Payment tracking
    application_form_paid BOOLEAN NOT NULL DEFAULT FALSE,
    advance_payment_completed BOOLEAN NOT NULL DEFAULT FALSE,
    advance_payment_paid BOOLEAN NOT NULL DEFAULT FALSE,
    advance_payment_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    advance_payment_date DATE,
    advance_balance NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    total_advance_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,

    -- Verification & Fees
    verified_tenant BOOLEAN NOT NULL DEFAULT FALSE,
    verified_stakeholder BOOLEAN NOT NULL DEFAULT FALSE,
    applicant_fee_paid BOOLEAN NOT NULL DEFAULT FALSE,
    treasurer_approved BOOLEAN NOT NULL DEFAULT FALSE,
    applicant_fee_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    applicant_fee_date DATE,
    verification_date TIMESTAMPTZ,

    -- Approval stages
    market_approval_status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    endorsement_status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    endorsing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    endorsement_remarks TEXT,
    endorsed_at TIMESTAMPTZ,
    bplo_status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    final_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    endorsed_by VARCHAR(120),
    bplo_approved_by VARCHAR(120),
    approval_date TIMESTAMPTZ,
    remarks TEXT,
    market_supervisor_approved BOOLEAN NOT NULL DEFAULT FALSE,
    bplo_approved BOOLEAN NOT NULL DEFAULT FALSE,
    endorsing_approved BOOLEAN NOT NULL DEFAULT FALSE,
    final_endorsed BOOLEAN NOT NULL DEFAULT FALSE,
    treasurer_paid BOOLEAN NOT NULL DEFAULT FALSE,

    -- Dates & Archive
    applied_on DATE DEFAULT CURRENT_DATE,
    approved_on DATE,
    archived_on DATE,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_stakeholder_user ON public.stakeholders(user_id);
CREATE INDEX IF NOT EXISTS idx_stakeholder_business_name ON public.stakeholders(business_name);
CREATE INDEX IF NOT EXISTS idx_stakeholder_application_status ON public.stakeholders(application_status);
CREATE INDEX IF NOT EXISTS idx_stakeholder_is_archived ON public.stakeholders(is_archived);

-- ==============================================================================
-- 7. STAKEHOLDER DOCUMENTS
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.stakeholder_documents (
    id BIGSERIAL PRIMARY KEY,
    stakeholder_id BIGINT NOT NULL REFERENCES public.stakeholders(id) ON DELETE CASCADE,
    document_type VARCHAR(100) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path TEXT NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_document_stakeholder ON public.stakeholder_documents(stakeholder_id);

-- ==============================================================================
-- 8. BUSINESS APPLICATIONS (Self-service applicant portal)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.business_applications (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    business_name VARCHAR(120) NOT NULL,
    business_type VARCHAR(80) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    middle_name VARCHAR(80),
    last_name VARCHAR(80) NOT NULL,
    contact VARCHAR(40) NOT NULL,
    email VARCHAR(120),
    address TEXT NOT NULL,
    selected_stall_id BIGINT REFERENCES public.stalls(id) ON DELETE SET NULL,

    application_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    onboarding_status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    application_form_paid BOOLEAN NOT NULL DEFAULT FALSE,
    advance_payment_completed BOOLEAN NOT NULL DEFAULT FALSE,
    advance_payment_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    advance_payment_date DATE,
    advance_balance NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    total_advance_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,

    market_approval_status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    endorsement_status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    endorsing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    endorsement_remarks TEXT,
    endorsed_at TIMESTAMPTZ,
    bplo_status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    final_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    endorsed_by VARCHAR(120),
    bplo_approved_by VARCHAR(120),
    approval_date TIMESTAMPTZ,
    remarks TEXT,
    market_supervisor_approved BOOLEAN NOT NULL DEFAULT FALSE,
    bplo_approved BOOLEAN NOT NULL DEFAULT FALSE,

    applied_on DATE DEFAULT CURRENT_DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_business_application_user ON public.business_applications(user_id);
CREATE INDEX IF NOT EXISTS idx_business_application_status ON public.business_applications(application_status);

-- ==============================================================================
-- 9. OCCUPANTS
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.occupants (
    id BIGSERIAL PRIMARY KEY,
    stakeholder_id BIGINT UNIQUE REFERENCES public.stakeholders(id) ON DELETE CASCADE,
    advance_balance NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    occupied_since TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    occupancy_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    contract_id BIGINT,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_occupant_stakeholder ON public.occupants(stakeholder_id);
CREATE INDEX IF NOT EXISTS idx_occupant_archived ON public.occupants(is_archived);

-- Link stall to occupant
ALTER TABLE public.stalls 
    DROP CONSTRAINT IF EXISTS fk_stalls_occupant;
ALTER TABLE public.stalls
    ADD CONSTRAINT fk_stalls_occupant 
    FOREIGN KEY (occupant_id) REFERENCES public.occupants(id) ON DELETE SET NULL;

-- ==============================================================================
-- 10. CONTRACTS
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.contracts (
    id BIGSERIAL PRIMARY KEY,
    occupant_id BIGINT NOT NULL REFERENCES public.occupants(id) ON DELETE RESTRICT,
    stall_id BIGINT NOT NULL REFERENCES public.stalls(id) ON DELETE RESTRICT,
    contract_no VARCHAR(80),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    monthly_rent NUMERIC(12, 2) NOT NULL,
    billing_frequency VARCHAR(30) NOT NULL DEFAULT 'MONTHLY',
    terms TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_contract_occupant ON public.contracts(occupant_id);
CREATE INDEX IF NOT EXISTS idx_contract_stall ON public.contracts(stall_id);
CREATE INDEX IF NOT EXISTS idx_contract_status ON public.contracts(status);
CREATE INDEX IF NOT EXISTS idx_contract_end_date ON public.contracts(end_date);

-- ==============================================================================
-- 11. BILLINGS
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.billings (
    id BIGSERIAL PRIMARY KEY,
    occupant_id BIGINT NOT NULL REFERENCES public.occupants(id) ON DELETE RESTRICT,
    contract_id BIGINT REFERENCES public.contracts(id) ON DELETE SET NULL,
    billing_no VARCHAR(100),
    billing_period VARCHAR(100),
    total_amount NUMERIC(12, 2) NOT NULL,
    paid_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    balance NUMERIC(12, 2) NOT NULL,
    due_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'UNPAID',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_billing_occupant ON public.billings(occupant_id);
CREATE INDEX IF NOT EXISTS idx_billing_contract ON public.billings(contract_id);
CREATE INDEX IF NOT EXISTS idx_billing_status ON public.billings(status);
CREATE INDEX IF NOT EXISTS idx_billing_due_date ON public.billings(due_date);

-- ==============================================================================
-- 12. PAYMENTS
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.payments (
    id BIGSERIAL PRIMARY KEY,
    rent_cycle VARCHAR(100),
    billing_id BIGINT REFERENCES public.billings(id) ON DELETE SET NULL,
    stakeholder_id BIGINT NOT NULL REFERENCES public.stakeholders(id) ON DELETE RESTRICT,
    amount NUMERIC(12, 2) NOT NULL,
    receipt_no VARCHAR(100),
    reference_no VARCHAR(120),
    payment_type payment_type_enum NOT NULL,
    payment_date TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payment_billing ON public.payments(billing_id);
CREATE INDEX IF NOT EXISTS idx_payment_stakeholder ON public.payments(stakeholder_id);
CREATE INDEX IF NOT EXISTS idx_payment_date ON public.payments(payment_date);

-- Trigger to auto-update billing paid_amount and balance upon payment insert
CREATE OR REPLACE FUNCTION public.update_billing_on_payment()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.billing_id IS NOT NULL THEN
        UPDATE public.billings
        SET paid_amount = paid_amount + NEW.amount,
            balance = GREATEST(0, total_amount - (paid_amount + NEW.amount)),
            status = CASE 
                WHEN (total_amount - (paid_amount + NEW.amount)) <= 0 THEN 'PAID'
                ELSE 'PARTIAL'
            END
        WHERE id = NEW.billing_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trg_update_billing_on_payment ON public.payments;
CREATE TRIGGER trg_update_billing_on_payment
    AFTER INSERT ON public.payments
    FOR EACH ROW EXECUTE FUNCTION public.update_billing_on_payment();

-- ==============================================================================
-- 13. NOTIFICATIONS
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.notifications (
    id BIGSERIAL PRIMARY KEY,
    stakeholder_id BIGINT REFERENCES public.stakeholders(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    explanation TEXT,
    recommendation TEXT,
    priority VARCHAR(30) NOT NULL DEFAULT 'LOW',
    notification_type VARCHAR(100),
    related_record_type VARCHAR(100),
    related_record_id BIGINT,
    ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notification_stakeholder ON public.notifications(stakeholder_id);
CREATE INDEX IF NOT EXISTS idx_notification_created_at ON public.notifications(created_at);

-- ==============================================================================
-- 14. APPROVAL HISTORY
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.approval_history (
    id BIGSERIAL PRIMARY KEY,
    stakeholder_id BIGINT NOT NULL REFERENCES public.stakeholders(id) ON DELETE CASCADE,
    stage VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    approved_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    remarks TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_approval_history_stakeholder ON public.approval_history(stakeholder_id);

-- ==============================================================================
-- 15. AUDIT LOGS
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    role VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_log_user ON public.audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON public.audit_logs(created_at);

-- ==============================================================================
-- 16. SYSTEM SETTINGS
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.system_settings (
    id BIGINT PRIMARY KEY DEFAULT 1,
    system_name VARCHAR(255) DEFAULT 'Rental Management System for the Public Market of Manticao',
    municipality VARCHAR(100) DEFAULT 'Manticao',
    office VARCHAR(100) DEFAULT 'Public Market Office',
    contact VARCHAR(50) DEFAULT '',
    email_address VARCHAR(120) DEFAULT '',
    billing_frequency VARCHAR(50) DEFAULT 'MONTHLY',
    advance_payment_period INT DEFAULT 0,
    grace_period INT DEFAULT 0,
    currency VARCHAR(10) DEFAULT 'PHP',
    application_notifications BOOLEAN DEFAULT TRUE,
    payment_notifications BOOLEAN DEFAULT TRUE,
    billing_reminders BOOLEAN DEFAULT TRUE,
    contract_expiration_alerts BOOLEAN DEFAULT TRUE,
    CONSTRAINT single_row_settings CHECK (id = 1)
);

-- Initialize default system settings row if not present
INSERT INTO public.system_settings (id)
VALUES (1)
ON CONFLICT (id) DO NOTHING;

-- ==============================================================================
-- 17. ROW LEVEL SECURITY (RLS) POLICIES
-- ==============================================================================

-- Enable RLS on all tables
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.stall_types ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.stalls ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.rental_rates ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.stakeholders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.stakeholder_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.business_applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.occupants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.contracts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.billings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.approval_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.system_settings ENABLE ROW LEVEL SECURITY;

-- ------------------------------------------------------------------------------
-- Profiles Policies
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "Users can read own profile or Admins read all" ON public.profiles;
CREATE POLICY "Users can read own profile or Admins read all" ON public.profiles
    FOR SELECT USING (auth.uid() = id OR public.is_admin());

DROP POLICY IF EXISTS "Users can update own name/username" ON public.profiles;
CREATE POLICY "Users can update own name/username" ON public.profiles
    FOR UPDATE USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id AND role = (SELECT role FROM public.profiles WHERE id = auth.uid()));

DROP POLICY IF EXISTS "Admins have full access to profiles" ON public.profiles;
CREATE POLICY "Admins have full access to profiles" ON public.profiles
    FOR ALL USING (public.is_admin());

-- ------------------------------------------------------------------------------
-- Stalls & Stall Types & Rental Rates (Publicly readable, Admin modifiable)
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "Anyone can view stall types" ON public.stall_types;
CREATE POLICY "Anyone can view stall types" ON public.stall_types
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "Admins manage stall types" ON public.stall_types;
CREATE POLICY "Admins manage stall types" ON public.stall_types
    FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Anyone can view stalls" ON public.stalls;
CREATE POLICY "Anyone can view stalls" ON public.stalls
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "Admins manage stalls" ON public.stalls;
CREATE POLICY "Admins manage stalls" ON public.stalls
    FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Anyone can view rental rates" ON public.rental_rates;
CREATE POLICY "Anyone can view rental rates" ON public.rental_rates
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "Admins manage rental rates" ON public.rental_rates;
CREATE POLICY "Admins manage rental rates" ON public.rental_rates
    FOR ALL USING (public.is_admin());

-- ------------------------------------------------------------------------------
-- Business Applications (Applicant self-service)
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "Users can view their own application" ON public.business_applications;
CREATE POLICY "Users can view their own application" ON public.business_applications
    FOR SELECT USING (auth.uid() = user_id OR public.is_admin());

DROP POLICY IF EXISTS "Users can insert their own application" ON public.business_applications;
CREATE POLICY "Users can insert their own application" ON public.business_applications
    FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update their pending application" ON public.business_applications;
CREATE POLICY "Users can update their pending application" ON public.business_applications
    FOR UPDATE USING (auth.uid() = user_id OR public.is_admin())
    WITH CHECK (auth.uid() = user_id OR public.is_admin());

-- ------------------------------------------------------------------------------
-- Stakeholders & Documents
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "Stakeholders read own data" ON public.stakeholders;
CREATE POLICY "Stakeholders read own data" ON public.stakeholders
    FOR SELECT USING (auth.uid() = user_id OR public.is_admin());

DROP POLICY IF EXISTS "Admins manage stakeholders" ON public.stakeholders;
CREATE POLICY "Admins manage stakeholders" ON public.stakeholders
    FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Read stakeholder documents" ON public.stakeholder_documents;
CREATE POLICY "Read stakeholder documents" ON public.stakeholder_documents
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.stakeholders s
            WHERE s.id = stakeholder_documents.stakeholder_id
            AND (s.user_id = auth.uid() OR public.is_admin())
        )
    );

DROP POLICY IF EXISTS "Upload stakeholder documents" ON public.stakeholder_documents;
CREATE POLICY "Upload stakeholder documents" ON public.stakeholder_documents
    FOR INSERT WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.stakeholders s
            WHERE s.id = stakeholder_documents.stakeholder_id
            AND (s.user_id = auth.uid() OR public.is_admin())
        )
    );

DROP POLICY IF EXISTS "Admins manage documents" ON public.stakeholder_documents;
CREATE POLICY "Admins manage documents" ON public.stakeholder_documents
    FOR ALL USING (public.is_admin());

-- ------------------------------------------------------------------------------
-- Occupants, Contracts, Billings, Payments
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "Occupants read own data" ON public.occupants;
CREATE POLICY "Occupants read own data" ON public.occupants
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.stakeholders s
            WHERE s.id = occupants.stakeholder_id
            AND (s.user_id = auth.uid() OR public.is_admin())
        )
    );

DROP POLICY IF EXISTS "Admins manage occupants" ON public.occupants;
CREATE POLICY "Admins manage occupants" ON public.occupants
    FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Contracts readable by occupant or admin" ON public.contracts;
CREATE POLICY "Contracts readable by occupant or admin" ON public.contracts
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.occupants o
            JOIN public.stakeholders s ON s.id = o.stakeholder_id
            WHERE o.id = contracts.occupant_id
            AND (s.user_id = auth.uid() OR public.is_admin())
        )
    );

DROP POLICY IF EXISTS "Admins manage contracts" ON public.contracts;
CREATE POLICY "Admins manage contracts" ON public.contracts
    FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Billings readable by occupant or admin" ON public.billings;
CREATE POLICY "Billings readable by occupant or admin" ON public.billings
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.occupants o
            JOIN public.stakeholders s ON s.id = o.stakeholder_id
            WHERE o.id = billings.occupant_id
            AND (s.user_id = auth.uid() OR public.is_admin())
        )
    );

DROP POLICY IF EXISTS "Admins manage billings" ON public.billings;
CREATE POLICY "Admins manage billings" ON public.billings
    FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Payments readable by stakeholder or admin" ON public.payments;
CREATE POLICY "Payments readable by stakeholder or admin" ON public.payments
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.stakeholders s
            WHERE s.id = payments.stakeholder_id
            AND (s.user_id = auth.uid() OR public.is_admin())
        )
    );

DROP POLICY IF EXISTS "Admins manage payments" ON public.payments;
CREATE POLICY "Admins manage payments" ON public.payments
    FOR ALL USING (public.is_admin());

-- ------------------------------------------------------------------------------
-- Notifications
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "Read notifications" ON public.notifications;
CREATE POLICY "Read notifications" ON public.notifications
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.stakeholders s
            WHERE s.id = notifications.stakeholder_id
            AND (s.user_id = auth.uid() OR public.is_admin())
        )
    );

DROP POLICY IF EXISTS "Update own notifications (mark read)" ON public.notifications;
CREATE POLICY "Update own notifications (mark read)" ON public.notifications
    FOR UPDATE USING (
        EXISTS (
            SELECT 1 FROM public.stakeholders s
            WHERE s.id = notifications.stakeholder_id
            AND (s.user_id = auth.uid() OR public.is_admin())
        )
    );

DROP POLICY IF EXISTS "Admins manage notifications" ON public.notifications;
CREATE POLICY "Admins manage notifications" ON public.notifications
    FOR ALL USING (public.is_admin());

-- ------------------------------------------------------------------------------
-- Approval History & Audit Logs & System Settings
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "Admins and managers view approval history" ON public.approval_history;
CREATE POLICY "Admins and managers view approval history" ON public.approval_history
    FOR SELECT USING (public.is_admin());

DROP POLICY IF EXISTS "Admins manage approval history" ON public.approval_history;
CREATE POLICY "Admins manage approval history" ON public.approval_history
    FOR ALL USING (public.is_admin());

DROP POLICY IF EXISTS "Admins view audit logs" ON public.audit_logs;
CREATE POLICY "Admins view audit logs" ON public.audit_logs
    FOR SELECT USING (public.is_admin());

DROP POLICY IF EXISTS "Authenticated users can create audit logs" ON public.audit_logs;
CREATE POLICY "Authenticated users can create audit logs" ON public.audit_logs
    FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);

DROP POLICY IF EXISTS "Anyone can read system settings" ON public.system_settings;
CREATE POLICY "Anyone can read system settings" ON public.system_settings
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "Admins update system settings" ON public.system_settings;
CREATE POLICY "Admins update system settings" ON public.system_settings
    FOR UPDATE USING (public.is_admin());

