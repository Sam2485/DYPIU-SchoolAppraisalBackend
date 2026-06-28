ALTER TABLE public.scholarship_summary
    ADD COLUMN IF NOT EXISTS attachment TEXT;

ALTER TABLE public.scholarship_students
    ADD COLUMN IF NOT EXISTS attachment TEXT;

ALTER TABLE public.hackathons
    ADD COLUMN IF NOT EXISTS attachment TEXT;

ALTER TABLE public.cultural_activities
    ADD COLUMN IF NOT EXISTS attachment TEXT;

ALTER TABLE public.sports_activities
    ADD COLUMN IF NOT EXISTS attachment TEXT;

ALTER TABLE public.community_activities
    ADD COLUMN IF NOT EXISTS attachment TEXT;

ALTER TABLE public.admin_student_awards
    ADD COLUMN IF NOT EXISTS attachment TEXT;
