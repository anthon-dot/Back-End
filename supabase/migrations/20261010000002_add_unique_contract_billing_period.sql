-- ==============================================================================
-- Migration: Add Unique Constraint / Index on Contract ID and Billing Period
-- Prevents duplicate initial and recurring billings for the same contract and period.
-- ==============================================================================

CREATE UNIQUE INDEX IF NOT EXISTS uq_billings_contract_period
ON public.billings(contract_id, billing_period)
WHERE contract_id IS NOT NULL;
