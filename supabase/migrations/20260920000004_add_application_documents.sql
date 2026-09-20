-- Migration: Add document url columns to business_applications and enable stakeholder policies
ALTER TABLE public.business_applications 
ADD COLUMN IF NOT EXISTS id_document_url TEXT,
ADD COLUMN IF NOT EXISTS letter_document_url TEXT;

-- Enable stakeholder insert and update policies for self-application
DROP POLICY IF EXISTS "Users can insert own stakeholder record" ON public.stakeholders;
CREATE POLICY "Users can insert own stakeholder record" ON public.stakeholders
    FOR INSERT WITH CHECK (auth.uid() = user_id OR public.is_admin());

DROP POLICY IF EXISTS "Users can update own stakeholder record" ON public.stakeholders;
CREATE POLICY "Users can update own stakeholder record" ON public.stakeholders
    FOR UPDATE USING (auth.uid() = user_id OR public.is_admin())
    WITH CHECK (auth.uid() = user_id OR public.is_admin());
