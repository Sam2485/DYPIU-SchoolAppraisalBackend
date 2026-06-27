ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS root_submission_id BIGINT;
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS parent_submission_id BIGINT;
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS previous_approved_submission_id BIGINT;
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS audit_cycle VARCHAR(100);
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS report_category VARCHAR(100);
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS approved_by_user_id BIGINT;
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS approved_by_name VARCHAR(255);
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS approved_by_role VARCHAR(255);
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS approved_by_designation VARCHAR(255);
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS created_from_version INT;

CREATE UNIQUE INDEX IF NOT EXISTS uk_submissions_root_version
    ON public.submissions(root_submission_id, version)
    WHERE root_submission_id IS NOT NULL;
