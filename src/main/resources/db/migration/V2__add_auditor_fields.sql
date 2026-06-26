-- Add auditor support to users and submissions tables

-- User table additions
ALTER TABLE public.users ADD COLUMN account_type VARCHAR(100);
ALTER TABLE public.users ADD COLUMN category VARCHAR(100);
ALTER TABLE public.users ADD COLUMN auditor_type VARCHAR(100);
ALTER TABLE public.users ADD COLUMN auditor_role VARCHAR(255);
ALTER TABLE public.users ADD COLUMN post VARCHAR(255);
ALTER TABLE public.users ADD COLUMN status VARCHAR(100) DEFAULT 'active';

-- Submission table additions for forwarding and auditor reviews
ALTER TABLE public.submissions ADD COLUMN forwarded_to_auditor_id BIGINT;
ALTER TABLE public.submissions ADD COLUMN forwarded_to_auditor_name VARCHAR(255);
ALTER TABLE public.submissions ADD COLUMN forwarded_to_auditor_email VARCHAR(255);

ALTER TABLE public.submissions ADD COLUMN forwarded_to_auditor_ids TEXT;
ALTER TABLE public.submissions ADD COLUMN forwarded_to_auditor_names TEXT;
ALTER TABLE public.submissions ADD COLUMN forwarded_to_auditor_emails TEXT;

ALTER TABLE public.submissions ADD COLUMN forwarded_auditor_type VARCHAR(100);
ALTER TABLE public.submissions ADD COLUMN forwarded_audit_category VARCHAR(100);
ALTER TABLE public.submissions ADD COLUMN forwarded_at TIMESTAMP;

ALTER TABLE public.submissions ADD COLUMN auditor_reviewed_by VARCHAR(255);
ALTER TABLE public.submissions ADD COLUMN auditor_reviewed_by_designation VARCHAR(255);
ALTER TABLE public.submissions ADD COLUMN auditor_reviewed_by_role VARCHAR(255);
ALTER TABLE public.submissions ADD COLUMN auditor_reviewed_on TIMESTAMP;
