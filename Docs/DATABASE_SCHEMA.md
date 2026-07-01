# Database Schema & Migrations Documentation

This document explains the database structure, entity models, and migration scripts implemented for the School Appraisal System.

## Database Engine
- **Database Server**: PostgreSQL (GCP Cloud SQL / local server on port 5432).
- **ORM Framework**: Spring Data JPA / Hibernate.
- **Migration Engine**: Flyway (ddl-auto: validate).

---

## 1. Core Schema Tables

### users
Holds authentication and user metadata.
- `id` (BIGINT, PRIMARY KEY, SERIAL): Unique identifier.
- `email` (VARCHAR(255), UNIQUE, NOT NULL): Login ID.
- `password` (VARCHAR(255), NOT NULL): BCrypt hashed password.
- `name` (VARCHAR(255)): Full Name of the user.
- `designation` (VARCHAR(255)): Position title (e.g. Director, Registrar).
- `school` (VARCHAR(255)): Mapped School or Office.
- `role` (VARCHAR(100), NOT NULL): System role (`director`, `administrative`, `vice-chancellor`, `iqac`).

### submissions
Holds the master state of academic and administrative audit form submissions.
- `id` (BIGINT, PRIMARY KEY, SERIAL): Unique identifier.
- `email` (VARCHAR(255), NOT NULL): Mail ID of the submitter.
- `audit_type` (VARCHAR(100), NOT NULL): `academic` or `administrative`.
- `school` (VARCHAR(255)): Submitter's school/office.
- `submitted_by` (VARCHAR(255)): Submitter's name.
- `submitted_at` (TIMESTAMP): Submission date-time.
- `status` (VARCHAR(100), NOT NULL): Form status (`DRAFT`, `SUBMITTED`, `UNDER_REVIEW`, `APPROVED`, `SENT_BACK`).
- `remarks` (TEXT): Reviewer comments.
- `reviewed_by` (VARCHAR(255)): Name of the reviewer (VC / IQAC).
- `reviewed_at` (TIMESTAMP): Date-time of review.
- `values_data` (TEXT): JSON payload storing all custom form fields.
- `tables_data` (TEXT): JSON payload storing custom table structures.
- `attachments` (TEXT): JSON payload list of attachments.
- `submitted_by_details` (TEXT): JSON payload tracking submission status, timestamp, name, and email details for each administrative role.
- `version` (INT, DEFAULT 1): Incremental version number for locking.

### snapshots
Saves history snapshots of form entries every time the user saves drafts or submits.
- `id` (BIGINT, PRIMARY KEY, SERIAL): Unique identifier.
- `submission_id` (BIGINT, NOT NULL): Links to submissions.
- `saved_at` (TIMESTAMP, NOT NULL): Capture time.
- `status` (VARCHAR(100), NOT NULL): Captured status.
- `values_data` (TEXT): JSON fields state.
- `tables_data` (TEXT): JSON tables state.
- `attachments` (TEXT): JSON attachments list state.
- `version` (INT, NOT NULL): Snapshot version number.

### password_reset_tokens
Holds tokens generated for password resets.
- `id` (BIGINT, PRIMARY KEY, SERIAL): Unique identifier.
- `email` (VARCHAR(255), NOT NULL): Target email of the user.
- `token_hash` (VARCHAR(255), UNIQUE, NOT NULL): SHA-256 hashed reset token.
- `used` (BOOLEAN, DEFAULT FALSE, NOT NULL): Status of token consumption.
- `expires_at` (TIMESTAMP, NOT NULL): Date-time when token expires.
- `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP): Creation date-time.

---

## 2. Dynamic Child Audit Tables (64 Tables)
To support granular relational mapping, 64 distinct child tables have been created for specific audit sections.

Each of these tables maps to:
- `id` (BIGINT PRIMARY KEY, SERIAL): Row identity.
- `submission_id` (BIGINT, NOT NULL): Foreign key referencing `public.submissions(id) ON DELETE CASCADE`.
- Data Columns: Text columns storing row cell contents.

### Mapped Tables Catalog:
1. `student_strength`
2. `faculty_strength`
3. `board_of_studies`
4. `syllabus_revision`
5. `obe_implementation`
6. `nep_status`
7. `best_practices`
8. `student_mentoring`
9. `graduating_students`
10. `success_rate`
11. `qualifying_exams`
12. `student_awards`
13. `student_placements`
14. `higher_studies`
15. `student_startups`
16. `student_courses`
17. `alumni_interactions`
18. `guest_lectures`
19. `professional_bodies`
20. `value_added_courses`
21. `career_guidance`
22. `extension_activities`
23. `faculty_specialization`
24. `research_publications`
25. `books_chapters`
26. `corporate_training`
27. `consultancy`
28. `research_funds`
29. `e_contents`
30. `teacher_awards`
31. `patents_copyrights`
32. `fdp_organized`
33. `fdp_attended`
34. `functional_mous`
35. `swoc_strength`
36. `swoc_weaknesses`
37. `swoc_opportunities`
38. `swoc_challenges`
39. `swoc_other_information`
40. `courses_offered`
41. `student_statistics`
42. `statutory_bodies`
43. `audit_records`
44. `scholarship_summary`
45. `scholarship_students`
46. `faculty_information`
47. `faculty_tenure`
48. `faculty_experience`
49. `supporting_staff`
50. `staff_training`
51. `building_infrastructure`
52. `library_infrastructure`
53. `e_resources`
54. `it_infrastructure`
55. `sports_facilities`
56. `divyangajan_facilities`
57. `research_resources`
58. `hackathons`
59. `cultural_activities`
60. `sports_activities`
61. `community_activities`
62. `admin_student_awards`
63. `training_activities`
64. `industry_collaborations`

---

## 3. Database Migrations
Migrations are baseline managed inside the directory `src/main/resources/db/migration`:
- **V1__init_schema.sql**: Declares DDL for core tables (including `users`, `submissions`, `snapshots`, and `password_reset_tokens`) and all 64 child audit tables with clean cascade constraints.
- **V2__add_auditor_fields.sql**: Adds columns for forwarding submissions to single auditors.
- **V3__ensure_auditor_fields_exist.sql**: Ensures the forwarding columns are correctly defined.
- **V4__add_submission_report_version_fields.sql**: Adds support for versions, next versions, parent-child links, and root submission history indexing.
- **V5__add_next_cycle_fields.sql**: Adds indicators to track if a submission has a successor audit cycle.
- **V6__add_academic_years_and_auditor_assignments.sql**: Adds the `academic_years` table, `submission_auditor_assignments` junction table, and composite key constraints.
- **V7__add_attachment_school_group_and_auditor_posts.sql**: Adds `school_group`, `administrative_post`, and JSON columns for multi-auditor routing.
- **V8__add_administrative_attachment_columns.sql**: Adds section-specific attachment tracking columns.
- **V9__add_indexes.sql**: Configures performance-optimizing database indexes on highly queried fields to avoid full-table scans.
- **V10__add_submitted_by_details.sql**: Adds the `submitted_by_details` text column to track granular contributor submissions in shared administrative audits.
- **V11__add_courses_offered_columns.sql**: Adds `students_admitted` and `attachment` columns to the `courses_offered` table for Section A.
- **V12__add_staff_training_attachment.sql**: Adds the `attachment` column to the `staff_training` table for Section B.

---

## 4. Query Performance Indexes (Added in V9)
The following database indexes are configured to optimize lookup times under load:
1. `idx_submissions_email_audit_type_year`: Composite index on `public.submissions(email, audit_type, academic_year)` to speed up status lookups, history fetches, and draft check queries.
2. `idx_submissions_status`: B-tree index on `public.submissions(status)` to optimize reviewer/IQAC dashboard queries.
3. `idx_submissions_root_parent_id`: B-tree index on `public.submissions(root_submission_id, parent_submission_id)` to speed up version history and lineage fetches.
4. `idx_users_role` / `idx_users_status`: B-tree indexes on `public.users(role)` and `public.users(status)` to optimize authority verification.

