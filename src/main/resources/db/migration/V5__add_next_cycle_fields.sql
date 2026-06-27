ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS has_next_cycle BOOLEAN DEFAULT FALSE;
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS next_version_id BIGINT;

CREATE UNIQUE INDEX IF NOT EXISTS uk_submissions_parent_submission_id
    ON public.submissions(parent_submission_id)
    WHERE parent_submission_id IS NOT NULL;
