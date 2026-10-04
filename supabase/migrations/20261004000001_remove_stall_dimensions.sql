-- Migration: Ensure dimension and dimensions columns are removed from public.stalls
ALTER TABLE public.stalls DROP COLUMN IF EXISTS dimensions;
ALTER TABLE public.stalls DROP COLUMN IF EXISTS dimension;
