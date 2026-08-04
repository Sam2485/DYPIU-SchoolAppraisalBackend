-- Migration to support School of Humanities and Social Sciences (SOHSS)
UPDATE public.submissions
SET school_group = 'nonEngineering'
WHERE audit_type = 'academic'
  AND (UPPER(school) = 'SOHSS' 
       OR lower(school) LIKE '%humanities%'
       OR lower(school) LIKE '%social sciences%')
  AND (school_group IS NULL OR school_group != 'nonEngineering');

UPDATE public.snapshots sn
SET school_group = s.school_group
FROM public.submissions s
WHERE sn.submission_id = s.id
  AND (UPPER(s.school) = 'SOHSS' OR lower(s.school) LIKE '%humanities%' OR lower(s.school) LIKE '%social sciences%');
