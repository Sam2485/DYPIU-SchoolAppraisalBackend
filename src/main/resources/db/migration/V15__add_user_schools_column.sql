-- V15__add_user_schools_column.sql
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS schools VARCHAR(1000);
