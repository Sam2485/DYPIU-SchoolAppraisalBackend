ALTER TABLE public.submissions ADD COLUMN IF NOT EXISTS school_group VARCHAR(50);
ALTER TABLE public.snapshots ADD COLUMN IF NOT EXISTS academic_year VARCHAR(20);
ALTER TABLE public.snapshots ADD COLUMN IF NOT EXISTS audit_cycle VARCHAR(20);
ALTER TABLE public.snapshots ADD COLUMN IF NOT EXISTS school_group VARCHAR(50);

UPDATE public.submissions
SET academic_year = substring(academic_year from 1 for 5) || '20' || substring(academic_year from 6 for 2)
WHERE academic_year ~ '^[0-9]{4}-[0-9]{2}$';

UPDATE public.submissions
SET audit_cycle = substring(academic_year from 1 for 4) || '-' || substring(academic_year from 8 for 2)
WHERE academic_year ~ '^[0-9]{4}-[0-9]{4}$'
  AND (audit_cycle IS NULL OR audit_cycle = academic_year);

UPDATE public.submissions
SET school_group = CASE
    WHEN UPPER(school) IN ('SOCSEA', 'SOBB', 'SOCE', 'SOEMR')
        OR lower(school) LIKE '%computer science%'
        OR lower(school) LIKE '%bio%'
        OR lower(school) LIKE '%continual%'
        OR lower(school) LIKE '%engineering, management%'
        OR lower(school) LIKE '%engg%management%research%'
        THEN 'engineering'
    WHEN UPPER(school) IN ('SOCM', 'SOMCS', 'SOD', 'SOAA')
        OR lower(school) LIKE '%commerce%'
        OR lower(school) LIKE '%media%'
        OR lower(school) LIKE '%design%'
        OR lower(school) LIKE '%applied arts%'
        THEN 'nonEngineering'
    ELSE school_group
END
WHERE audit_type = 'academic'
  AND school_group IS NULL;

UPDATE public.snapshots sn
SET academic_year = s.academic_year,
    audit_cycle = s.audit_cycle,
    school_group = s.school_group
FROM public.submissions s
WHERE sn.submission_id = s.id;

CREATE TABLE IF NOT EXISTS public.user_administrative_posts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post VARCHAR(100) NOT NULL,
    CONSTRAINT fk_user_administrative_posts_user
        FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_administrative_posts
    ON public.user_administrative_posts(user_id, post);

INSERT INTO public.user_administrative_posts(user_id, post)
SELECT id, lower(trim(post))
FROM public.users
WHERE account_type = 'auditor'
  AND category = 'administrative'
  AND post IS NOT NULL
  AND trim(post) <> ''
ON CONFLICT DO NOTHING;
