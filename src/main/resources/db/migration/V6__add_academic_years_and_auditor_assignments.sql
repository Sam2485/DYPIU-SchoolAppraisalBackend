ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS academic_year VARCHAR(20);
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS administrative_post VARCHAR(100);

UPDATE public.submissions
SET academic_year = audit_cycle
WHERE academic_year IS NULL
  AND audit_cycle IS NOT NULL;

UPDATE public.submissions
SET status = 'DRAFT'
WHERE status = 'SENT_BACK';

CREATE TABLE IF NOT EXISTS public.academic_years (
    id BIGSERIAL PRIMARY KEY,
    year_label VARCHAR(20) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    started_at TIMESTAMP,
    closed_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_academic_years_one_active
    ON public.academic_years(active)
    WHERE active = TRUE;

INSERT INTO public.academic_years (year_label, active, started_at)
SELECT '2025-2026', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM public.academic_years WHERE active = TRUE);

CREATE TABLE IF NOT EXISTS public.submission_auditor_assignments (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    auditor_id BIGINT NOT NULL,
    auditor_name VARCHAR(255),
    auditor_email VARCHAR(255),
    auditor_type VARCHAR(100),
    category VARCHAR(100),
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_submission_auditor_assignment_submission
        FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE,
    CONSTRAINT fk_submission_auditor_assignment_auditor
        FOREIGN KEY (auditor_id) REFERENCES public.users(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_submission_auditor_assignment
    ON public.submission_auditor_assignments(submission_id, auditor_id);
