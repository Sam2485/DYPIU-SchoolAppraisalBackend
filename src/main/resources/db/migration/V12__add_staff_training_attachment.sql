-- Add attachment column to staff_training table for Event Report attachment
ALTER TABLE public.staff_training
ADD COLUMN IF NOT EXISTS attachment TEXT;
