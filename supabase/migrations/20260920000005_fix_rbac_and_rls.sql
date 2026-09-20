-- ==============================================================================
-- Migration: Fix RBAC, Staff Access, and RLS for All Roles (Admin, Treasurer, MS, BPLO, Endorsing)
-- ==============================================================================

-- 1. Helper functions to retrieve user role without infinite recursion
CREATE OR REPLACE FUNCTION public.get_current_user_role()
RETURNS text AS $$
DECLARE
    jwt_role text;
    db_role text;
BEGIN
    -- Check JWT metadata first (instant in-memory evaluation, 0 recursion)
    jwt_role := (auth.jwt() -> 'user_metadata' ->> 'role');
    IF jwt_role IS NOT NULL AND jwt_role <> '' THEN
        RETURN UPPER(jwt_role);
    END IF;

    -- Direct lookup in profiles
    SELECT role INTO db_role FROM public.profiles WHERE id = auth.uid();
    RETURN UPPER(COALESCE(db_role, ''));
END;
$$ LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public;

CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS boolean AS $$
BEGIN
    RETURN public.get_current_user_role() = 'ADMIN';
END;
$$ LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public;

CREATE OR REPLACE FUNCTION public.is_staff()
RETURNS boolean AS $$
DECLARE
    r text := public.get_current_user_role();
BEGIN
    RETURN r IN ('ADMIN', 'TREASURER', 'MARKET_SUPERVISOR', 'SUPERVISOR', 'BPLO', 'BPLO_OFFICE', 'ENDORSING_OFFICE', 'ENDORSING');
END;
$$ LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public;

-- 2. Profiles Policies
DROP POLICY IF EXISTS "Users can read own profile or Admins read all" ON public.profiles;
DROP POLICY IF EXISTS "Admins have full access to profiles" ON public.profiles;
DROP POLICY IF EXISTS "Users can update own name/username" ON public.profiles;
DROP POLICY IF EXISTS "Authenticated users view profiles" ON public.profiles;

CREATE POLICY "Authenticated users view profiles" ON public.profiles
    FOR SELECT TO authenticated USING (true);

CREATE POLICY "Users can update own name/username" ON public.profiles
    FOR UPDATE TO authenticated USING (auth.uid() = id);

CREATE POLICY "Admins have full access to profiles" ON public.profiles
    FOR ALL TO authenticated USING (public.is_admin());

-- 3. Business Applications Policies
DROP POLICY IF EXISTS "Users can view their own application" ON public.business_applications;
DROP POLICY IF EXISTS "Users can insert their own application" ON public.business_applications;
DROP POLICY IF EXISTS "Users can update their pending application" ON public.business_applications;
DROP POLICY IF EXISTS "Staff view all applications" ON public.business_applications;
DROP POLICY IF EXISTS "Staff manage all applications" ON public.business_applications;
DROP POLICY IF EXISTS "Staff or applicant view applications" ON public.business_applications;
DROP POLICY IF EXISTS "Staff or applicant insert applications" ON public.business_applications;
DROP POLICY IF EXISTS "Staff or applicant update applications" ON public.business_applications;

CREATE POLICY "Staff or applicant view applications" ON public.business_applications
    FOR SELECT TO authenticated USING (auth.uid() = user_id OR public.is_staff());

CREATE POLICY "Staff or applicant insert applications" ON public.business_applications
    FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id OR public.is_staff());

CREATE POLICY "Staff or applicant update applications" ON public.business_applications
    FOR UPDATE TO authenticated USING (auth.uid() = user_id OR public.is_staff());

-- 4. Stakeholders Policies
DROP POLICY IF EXISTS "Stakeholders read own data" ON public.stakeholders;
DROP POLICY IF EXISTS "Admins manage stakeholders" ON public.stakeholders;
DROP POLICY IF EXISTS "Users can insert own stakeholder record" ON public.stakeholders;
DROP POLICY IF EXISTS "Users can update own stakeholder record" ON public.stakeholders;
DROP POLICY IF EXISTS "Staff or stakeholder read stakeholders" ON public.stakeholders;
DROP POLICY IF EXISTS "Staff or user insert stakeholders" ON public.stakeholders;
DROP POLICY IF EXISTS "Staff or user update stakeholders" ON public.stakeholders;
DROP POLICY IF EXISTS "Admins delete stakeholders" ON public.stakeholders;

CREATE POLICY "Staff or stakeholder read stakeholders" ON public.stakeholders
    FOR SELECT TO authenticated USING (auth.uid() = user_id OR public.is_staff());

CREATE POLICY "Staff or user insert stakeholders" ON public.stakeholders
    FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id OR public.is_staff());

CREATE POLICY "Staff or user update stakeholders" ON public.stakeholders
    FOR UPDATE TO authenticated USING (auth.uid() = user_id OR public.is_staff());

CREATE POLICY "Admins delete stakeholders" ON public.stakeholders
    FOR DELETE TO authenticated USING (public.is_admin());

-- 5. Stakeholder Documents Policies
DROP POLICY IF EXISTS "Read stakeholder documents" ON public.stakeholder_documents;
DROP POLICY IF EXISTS "Upload stakeholder documents" ON public.stakeholder_documents;
DROP POLICY IF EXISTS "Admins manage documents" ON public.stakeholder_documents;
DROP POLICY IF EXISTS "Staff or stakeholder read documents" ON public.stakeholder_documents;
DROP POLICY IF EXISTS "Staff or stakeholder upload documents" ON public.stakeholder_documents;
DROP POLICY IF EXISTS "Staff manage documents" ON public.stakeholder_documents;

CREATE POLICY "Staff or stakeholder read documents" ON public.stakeholder_documents
    FOR SELECT TO authenticated USING (
        public.is_staff() OR EXISTS (
            SELECT 1 FROM public.stakeholders s
            WHERE s.id = stakeholder_documents.stakeholder_id AND s.user_id = auth.uid()
        )
    );

CREATE POLICY "Staff or stakeholder upload documents" ON public.stakeholder_documents
    FOR INSERT TO authenticated WITH CHECK (
        public.is_staff() OR EXISTS (
            SELECT 1 FROM public.stakeholders s
            WHERE s.id = stakeholder_documents.stakeholder_id AND s.user_id = auth.uid()
        )
    );

CREATE POLICY "Staff manage documents" ON public.stakeholder_documents
    FOR ALL TO authenticated USING (public.is_staff());

-- 6. Occupants Policies
DROP POLICY IF EXISTS "Occupants read own data" ON public.occupants;
DROP POLICY IF EXISTS "Admins manage occupants" ON public.occupants;
DROP POLICY IF EXISTS "Staff or occupant read occupants" ON public.occupants;
DROP POLICY IF EXISTS "Staff manage occupants" ON public.occupants;

CREATE POLICY "Staff or occupant read occupants" ON public.occupants
    FOR SELECT TO authenticated USING (
        public.is_staff() OR EXISTS (
            SELECT 1 FROM public.stakeholders s
            WHERE s.id = occupants.stakeholder_id AND s.user_id = auth.uid()
        )
    );

CREATE POLICY "Staff manage occupants" ON public.occupants
    FOR ALL TO authenticated USING (public.is_staff());

-- 7. Contracts Policies
DROP POLICY IF EXISTS "Contracts readable by occupant or admin" ON public.contracts;
DROP POLICY IF EXISTS "Admins manage contracts" ON public.contracts;
DROP POLICY IF EXISTS "Staff or occupant read contracts" ON public.contracts;
DROP POLICY IF EXISTS "Staff manage contracts" ON public.contracts;

CREATE POLICY "Staff or occupant read contracts" ON public.contracts
    FOR SELECT TO authenticated USING (
        public.is_staff() OR EXISTS (
            SELECT 1 FROM public.occupants o
            JOIN public.stakeholders s ON s.id = o.stakeholder_id
            WHERE o.id = contracts.occupant_id
            AND s.user_id = auth.uid()
        )
    );

CREATE POLICY "Staff manage contracts" ON public.contracts
    FOR ALL TO authenticated USING (public.is_staff());

-- 8. Billings Policies
DROP POLICY IF EXISTS "Billings readable by occupant or admin" ON public.billings;
DROP POLICY IF EXISTS "Admins manage billings" ON public.billings;
DROP POLICY IF EXISTS "Staff or occupant read billings" ON public.billings;
DROP POLICY IF EXISTS "Staff manage billings" ON public.billings;

CREATE POLICY "Staff or occupant read billings" ON public.billings
    FOR SELECT TO authenticated USING (
        public.is_staff() OR EXISTS (
            SELECT 1 FROM public.occupants o
            JOIN public.stakeholders s ON s.id = o.stakeholder_id
            WHERE o.id = billings.occupant_id
            AND s.user_id = auth.uid()
        )
    );

CREATE POLICY "Staff manage billings" ON public.billings
    FOR ALL TO authenticated USING (public.is_staff());

-- 9. Payments Policies
DROP POLICY IF EXISTS "Payments readable by stakeholder or admin" ON public.payments;
DROP POLICY IF EXISTS "Admins manage payments" ON public.payments;
DROP POLICY IF EXISTS "Staff or stakeholder read payments" ON public.payments;
DROP POLICY IF EXISTS "Staff manage payments" ON public.payments;

CREATE POLICY "Staff or stakeholder read payments" ON public.payments
    FOR SELECT TO authenticated USING (
        public.is_staff() OR EXISTS (
            SELECT 1 FROM public.stakeholders s
            WHERE s.id = payments.stakeholder_id AND s.user_id = auth.uid()
        )
    );

CREATE POLICY "Staff manage payments" ON public.payments
    FOR ALL TO authenticated USING (public.is_staff());

-- 10. Audit Logs Policies
DROP POLICY IF EXISTS "Admins view audit logs" ON public.audit_logs;
DROP POLICY IF EXISTS "Authenticated users can create audit logs" ON public.audit_logs;
DROP POLICY IF EXISTS "Staff view audit logs" ON public.audit_logs;
DROP POLICY IF EXISTS "Authenticated users create audit logs" ON public.audit_logs;

CREATE POLICY "Staff view audit logs" ON public.audit_logs
    FOR SELECT TO authenticated USING (public.is_staff());

CREATE POLICY "Authenticated users create audit logs" ON public.audit_logs
    FOR INSERT TO authenticated WITH CHECK (true);

-- 11. Stalls, Stall Types, Rental Rates
DROP POLICY IF EXISTS "Admins manage stalls" ON public.stalls;
DROP POLICY IF EXISTS "Admins manage stall types" ON public.stall_types;
DROP POLICY IF EXISTS "Admins manage rental rates" ON public.rental_rates;
DROP POLICY IF EXISTS "Staff manage stalls" ON public.stalls;
DROP POLICY IF EXISTS "Staff manage stall types" ON public.stall_types;
DROP POLICY IF EXISTS "Staff manage rental rates" ON public.rental_rates;

CREATE POLICY "Staff manage stalls" ON public.stalls
    FOR ALL TO authenticated USING (public.is_staff());

CREATE POLICY "Staff manage stall types" ON public.stall_types
    FOR ALL TO authenticated USING (public.is_staff());

CREATE POLICY "Staff manage rental rates" ON public.rental_rates
    FOR ALL TO authenticated USING (public.is_staff());
