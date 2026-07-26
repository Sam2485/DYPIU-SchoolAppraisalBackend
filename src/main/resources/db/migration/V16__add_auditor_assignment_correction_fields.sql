-- V16__add_auditor_assignment_correction_fields.sql
ALTER TABLE public.submission_auditor_assignments ADD COLUMN IF NOT EXISTS review_status VARCHAR(100);
ALTER TABLE public.submission_auditor_assignments ADD COLUMN IF NOT EXISTS requires_auditor_resubmission BOOLEAN DEFAULT FALSE;
ALTER TABLE public.submission_auditor_assignments ADD COLUMN IF NOT EXISTS correction_requested_for_auditor BOOLEAN DEFAULT FALSE;
ALTER TABLE public.submission_auditor_assignments ADD COLUMN IF NOT EXISTS auditor_correction_requested BOOLEAN DEFAULT FALSE;
ALTER TABLE public.submission_auditor_assignments ADD COLUMN IF NOT EXISTS auditor_correction_message TEXT;
ALTER TABLE public.submission_auditor_assignments ADD COLUMN IF NOT EXISTS auditor_correction_requested_by VARCHAR(255);
ALTER TABLE public.submission_auditor_assignments ADD COLUMN IF NOT EXISTS auditor_correction_requested_on TIMESTAMP;
