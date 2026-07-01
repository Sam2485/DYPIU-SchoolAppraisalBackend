-- Add new columns to courses_offered table for Students Admitted and Admitted Students List Attachment
ALTER TABLE public.courses_offered
ADD COLUMN IF NOT EXISTS students_admitted TEXT,
ADD COLUMN IF NOT EXISTS attachment TEXT;
