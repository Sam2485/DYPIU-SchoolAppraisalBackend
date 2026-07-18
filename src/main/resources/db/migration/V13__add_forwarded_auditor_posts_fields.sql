ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS forwarded_administrative_posts TEXT;
ALTER TABLE public.snapshots ADD COLUMN IF NOT EXISTS forwarded_administrative_posts TEXT;

ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS forwarded_to_auditor_posts TEXT;
ALTER TABLE public.snapshots ADD COLUMN IF NOT EXISTS forwarded_to_auditor_posts TEXT;
