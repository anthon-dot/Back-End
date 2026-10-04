-- Migration: Add optional section column to public.stalls if not exists
ALTER TABLE public.stalls ADD COLUMN IF NOT EXISTS section VARCHAR(100);
