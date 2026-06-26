-- Safety net for Cloud SQL databases that were changed manually or have partial Flyway history.

ALTER TABLE public.users ADD COLUMN IF NOT EXISTS account_type VARCHAR(100);
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS category VARCHAR(100);
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS auditor_type VARCHAR(100);
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS auditor_role VARCHAR(255);
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS post VARCHAR(255);
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS status VARCHAR(100) DEFAULT 'active';

ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS forwarded_to_auditor_id BIGINT;
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS forwarded_to_auditor_name VARCHAR(255);
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS forwarded_to_auditor_email VARCHAR(255);

ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS forwarded_to_auditor_ids TEXT;
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS forwarded_to_auditor_names TEXT;
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS forwarded_to_auditor_emails TEXT;

ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS forwarded_auditor_type VARCHAR(100);
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS forwarded_audit_category VARCHAR(100);
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS forwarded_at TIMESTAMP;

ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS auditor_reviewed_by VARCHAR(255);
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS auditor_reviewed_by_designation VARCHAR(255);
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS auditor_reviewed_by_role VARCHAR(255);
ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS auditor_reviewed_on TIMESTAMP;
