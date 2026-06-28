-- Add database indexes for frequently filtered and joined columns
CREATE INDEX IF NOT EXISTS idx_submissions_email_audit_type_year ON public.submissions(email, audit_type, academic_year);
CREATE INDEX IF NOT EXISTS idx_submissions_status ON public.submissions(status);
CREATE INDEX IF NOT EXISTS idx_submissions_root_parent_id ON public.submissions(root_submission_id, parent_submission_id);

CREATE INDEX IF NOT EXISTS idx_users_role ON public.users(role);
CREATE INDEX IF NOT EXISTS idx_users_status ON public.users(status);
