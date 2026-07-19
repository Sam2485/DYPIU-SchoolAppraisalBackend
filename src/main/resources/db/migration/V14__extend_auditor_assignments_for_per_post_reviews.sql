-- 1. Drop existing unique index on submission_auditor_assignments
DROP INDEX IF EXISTS public.uk_submission_auditor_assignment;

-- 2. Add columns to submission_auditor_assignments
ALTER TABLE public.submission_auditor_assignments ADD COLUMN IF NOT EXISTS post VARCHAR(255);
ALTER TABLE public.submission_auditor_assignments ADD COLUMN IF NOT EXISTS status VARCHAR(100) DEFAULT 'PENDING';
ALTER TABLE public.submission_auditor_assignments ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP;
ALTER TABLE public.submission_auditor_assignments ADD COLUMN IF NOT EXISTS values_data TEXT;
ALTER TABLE public.submission_auditor_assignments ADD COLUMN IF NOT EXISTS tables_data TEXT;
ALTER TABLE public.submission_auditor_assignments ADD COLUMN IF NOT EXISTS attachments TEXT;

-- 3. Create the new unique index on submission_auditor_assignments (coalescing post to empty string for safety)
CREATE UNIQUE INDEX IF NOT EXISTS uk_submission_auditor_assignment_post
    ON public.submission_auditor_assignments(submission_id, auditor_id, auditor_type, COALESCE(post, ''));

-- 4. Add auditor correction fields to public.submissions
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS auditor_correction_requested BOOLEAN DEFAULT FALSE;
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS correction_requested_for_auditor BOOLEAN DEFAULT FALSE;
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS requires_auditor_resubmission BOOLEAN DEFAULT FALSE;
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS auditor_correction_message TEXT;
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS auditor_correction_requested_by VARCHAR(255);
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS auditor_correction_requested_by_role VARCHAR(100);
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS auditor_correction_requested_on TIMESTAMP;
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS auditor_resubmitted_at TIMESTAMP;

-- 5. Add same auditor correction fields to public.snapshots (since snapshots mirror submissions)
ALTER TABLE public.snapshots ADD COLUMN IF NOT EXISTS auditor_correction_requested BOOLEAN DEFAULT FALSE;
ALTER TABLE public.snapshots ADD COLUMN IF NOT EXISTS correction_requested_for_auditor BOOLEAN DEFAULT FALSE;
ALTER TABLE public.snapshots ADD COLUMN IF NOT EXISTS requires_auditor_resubmission BOOLEAN DEFAULT FALSE;
ALTER TABLE public.snapshots ADD COLUMN IF NOT EXISTS auditor_correction_message TEXT;
ALTER TABLE public.snapshots ADD COLUMN IF NOT EXISTS auditor_correction_requested_by VARCHAR(255);
ALTER TABLE public.snapshots ADD COLUMN IF NOT EXISTS auditor_correction_requested_by_role VARCHAR(100);
ALTER TABLE public.snapshots ADD COLUMN IF NOT EXISTS auditor_correction_requested_on TIMESTAMP;
ALTER TABLE public.snapshots ADD COLUMN IF NOT EXISTS auditor_resubmitted_at TIMESTAMP;
