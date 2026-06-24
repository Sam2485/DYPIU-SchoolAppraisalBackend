-- Core tables for appraisal application

CREATE TABLE public.users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    designation VARCHAR(255),
    school VARCHAR(255),
    role VARCHAR(100) NOT NULL
);

CREATE TABLE public.submissions (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    audit_type VARCHAR(100) NOT NULL,
    school VARCHAR(255),
    submitted_by VARCHAR(255),
    submitted_at TIMESTAMP,
    status VARCHAR(100) NOT NULL,
    remarks TEXT,
    reviewed_by VARCHAR(255),
    reviewed_at TIMESTAMP,
    values_data TEXT,
    tables_data TEXT,
    attachments TEXT,
    version INT NOT NULL DEFAULT 1
);

CREATE TABLE public.snapshots (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    saved_at TIMESTAMP NOT NULL,
    status VARCHAR(100) NOT NULL,
    values_data TEXT,
    tables_data TEXT,
    attachments TEXT,
    version INT NOT NULL
);

-- Audit table schemas

CREATE TABLE public.student_strength (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    class_name TEXT,
    no_of_students TEXT,
    total TEXT,
    CONSTRAINT fk_student_strength_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.faculty_strength (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    required_faculty TEXT,
    available TEXT,
    CONSTRAINT fk_faculty_strength_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.board_of_studies (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    date_of_the_meeting TEXT,
    link_for_mom TEXT,
    CONSTRAINT fk_board_of_studies_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.syllabus_revision (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    category_of_feedback TEXT,
    link_for_analysis_and_atr TEXT,
    CONSTRAINT fk_syllabus_revision_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.obe_implementation (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    particular TEXT,
    link_for_the_document TEXT,
    CONSTRAINT fk_obe_implementation_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.nep_status (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sn TEXT,
    check_points TEXT,
    availability TEXT,
    link_for_the_document TEXT,
    CONSTRAINT fk_nep_status_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.best_practices (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sn TEXT,
    check_points TEXT,
    availability TEXT,
    link_for_the_document TEXT,
    CONSTRAINT fk_best_practices_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.student_mentoring (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    name_of_mentor TEXT,
    no_of_mentees TEXT,
    link_to_document TEXT,
    CONSTRAINT fk_student_mentoring_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.graduating_students (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    program TEXT,
    female TEXT,
    male TEXT,
    total TEXT,
    CONSTRAINT fk_graduating_students_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.success_rate (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    program TEXT,
    no_of_students_appeared_for_final_semester_exam TEXT,
    number_of_students_cleared_program_in_stipulated_duration TEXT,
    success_rate_percent TEXT,
    CONSTRAINT fk_success_rate_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.qualifying_exams (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    name_of_the_student TEXT,
    examination_details TEXT,
    proof_as_attachment TEXT,
    CONSTRAINT fk_qualifying_exams_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.student_awards (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    name_of_the_student TEXT,
    details_of_the_award TEXT,
    proof_as_an_attachment TEXT,
    CONSTRAINT fk_student_awards_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.student_placements (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    program TEXT,
    no_of_students_appeared_for_final_year_exam TEXT,
    no_of_students_placed TEXT,
    placement_percent TEXT,
    proof_as_attachment TEXT,
    CONSTRAINT fk_student_placements_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.higher_studies (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    program TEXT,
    no_of_students_appeared_for_final_year_exam TEXT,
    no_of_students_selected_for_higher_studies TEXT,
    students_percent TEXT,
    CONSTRAINT fk_higher_studies_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.student_startups (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sn TEXT,
    name_of_the_student TEXT,
    name_of_the_venture_start_up TEXT,
    link_to_relevant_proof TEXT,
    CONSTRAINT fk_student_startups_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.student_courses (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    name_of_student TEXT,
    year_of_study TEXT,
    name_of_course TEXT,
    duration TEXT,
    link_to_relevant_proof TEXT,
    CONSTRAINT fk_student_courses_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.alumni_interactions (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    name_of_alumni TEXT,
    designation TEXT,
    present_employer TEXT,
    date_on_interaction TEXT,
    topic TEXT,
    no_of_beneficiaries TEXT,
    link_to_relevant_proof TEXT,
    CONSTRAINT fk_alumni_interactions_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.guest_lectures (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    name_of_the_resource_person TEXT,
    designation_and_organization TEXT,
    date_of_conduction TEXT,
    topic TEXT,
    number_of_beneficiaries TEXT,
    link_to_relevant_proof TEXT,
    CONSTRAINT fk_guest_lectures_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.professional_bodies (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    name_of_the_professional_body_chapter_student_club TEXT,
    no_of_student_members TEXT,
    date_of_event_conduction TEXT,
    title_of_the_event TEXT,
    link_to_relevant_proof TEXT,
    CONSTRAINT fk_professional_bodies_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.value_added_courses (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    title_of_the_course TEXT,
    details_of_resource_person TEXT,
    duration_and_date_of_conduction TEXT,
    no_of_beneficiaries TEXT,
    link_to_relevant_proof TEXT,
    CONSTRAINT fk_value_added_courses_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.career_guidance (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    session_details TEXT,
    resource_person_details TEXT,
    date_of_conduction TEXT,
    number_of_beneficiaries TEXT,
    link_to_relevant_proof TEXT,
    CONSTRAINT fk_career_guidance_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.extension_activities (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    activity_details TEXT,
    organized_by TEXT,
    date_of_conduction TEXT,
    number_of_beneficiaries TEXT,
    link_to_relevant_proof TEXT,
    CONSTRAINT fk_extension_activities_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.faculty_specialization (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    name TEXT,
    designation TEXT,
    qualifications TEXT,
    specialization TEXT,
    no_of_phd_supervised TEXT,
    CONSTRAINT fk_faculty_specialization_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.research_publications (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    title_of_paper TEXT,
    name_of_author TEXT,
    name_of_journal TEXT,
    year_of_publication_with_volume_and_page TEXT,
    isbn_issn TEXT,
    indicate_ugc_approved_journal TEXT,
    national_international_journal TEXT,
    impact_factor TEXT,
    link_to_relevant_proof TEXT,
    CONSTRAINT fk_research_publications_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.books_chapters (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    name_of_the_teacher TEXT,
    title_of_the_book_chapters_published TEXT,
    title_of_the_paper TEXT,
    title_of_the_proceedings_of_the_conference TEXT,
    name_of_the_conference TEXT,
    national_international TEXT,
    year_of_publication TEXT,
    isbn_issn_number TEXT,
    name_of_the_publisher TEXT,
    link_to_relevant_proof TEXT,
    CONSTRAINT fk_books_chapters_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.corporate_training (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    name_of_faculty TEXT,
    agency_seeking_training TEXT,
    revenue_generated TEXT,
    number_of_trainees TEXT,
    link_to_relevant_proof TEXT,
    CONSTRAINT fk_corporate_training_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.consultancy (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    name_of_the_faculty TEXT,
    title_of_the_consultancy_project TEXT,
    consulting_sponsoring_agency TEXT,
    revenue_generated TEXT,
    link_to_relevant_proof TEXT,
    CONSTRAINT fk_consultancy_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.research_funds (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    name_of_the_project_endowments_chairs TEXT,
    name_of_the_principal_investigator TEXT,
    department_of_principal_investigator TEXT,
    year_of_award TEXT,
    funds_provided TEXT,
    duration_of_the_project TEXT,
    link_to_relevant_proof TEXT,
    CONSTRAINT fk_research_funds_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.e_contents (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    name_of_the_teacher TEXT,
    name_of_the_module TEXT,
    platform_on_which_module_is_developed TEXT,
    date_of_launching_e_content TEXT,
    link_to_relevant_proof TEXT,
    CONSTRAINT fk_e_contents_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.teacher_awards (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    name_of_the_teacher TEXT,
    national_awards TEXT,
    international_awards TEXT,
    link_to_relevant_proof TEXT,
    CONSTRAINT fk_teacher_awards_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.patents_copyrights (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    name_of_faculty_student TEXT,
    application_no TEXT,
    title_of_patent_copyright TEXT,
    date_of_filing TEXT,
    date_of_publication TEXT,
    date_of_award TEXT,
    link_to_relevant_proof TEXT,
    CONSTRAINT fk_patents_copyrights_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.fdp_organized (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sl_no TEXT,
    name_of_convener_coordinator TEXT,
    title_of_seminar_course TEXT,
    sponsoring_agency TEXT,
    duration_with_dates TEXT,
    no_of_internal_and_external_participants TEXT,
    proceedings_published_yes_no TEXT,
    link_to_relevant_proof TEXT,
    CONSTRAINT fk_fdp_organized_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.fdp_attended (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sl_no TEXT,
    name_of_faculty TEXT,
    title_of_seminar_course TEXT,
    sponsoring_agency_organization TEXT,
    duration_with_dates TEXT,
    date TEXT,
    link_to_relevant_proof TEXT,
    CONSTRAINT fk_fdp_attended_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.functional_mous (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    name_of_the_organization_institution_industry TEXT,
    year_of_signing_mou TEXT,
    duration_of_mou TEXT,
    list_the_actual_activities_under_each_mou TEXT,
    link_to_relevant_proof TEXT,
    CONSTRAINT fk_functional_mous_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.swoc_strength (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    details TEXT,
    CONSTRAINT fk_swoc_strength_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.swoc_weaknesses (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    details TEXT,
    CONSTRAINT fk_swoc_weaknesses_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.swoc_opportunities (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    details TEXT,
    CONSTRAINT fk_swoc_opportunities_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.swoc_challenges (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    details TEXT,
    CONSTRAINT fk_swoc_challenges_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.swoc_other_information (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    details TEXT,
    CONSTRAINT fk_swoc_other_information_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.courses_offered (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    name_of_the_program TEXT,
    level_ug_pg TEXT,
    intake TEXT,
    year_of_commencement_of_the_program TEXT,
    CONSTRAINT fk_courses_offered_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.student_statistics (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    category TEXT,
    ug TEXT,
    pg TEXT,
    phd TEXT,
    value_added_skill_courses TEXT,
    CONSTRAINT fk_student_statistics_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.statutory_bodies (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    body_cell TEXT,
    meetings_conducted TEXT,
    atr_status TEXT,
    remarks_link TEXT,
    CONSTRAINT fk_statutory_bodies_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.audit_records (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    audit_type TEXT,
    completed_yes_no TEXT,
    date TEXT,
    remarks_link TEXT,
    CONSTRAINT fk_audit_records_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.scholarship_summary (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    year TEXT,
    title_of_scholarship TEXT,
    number_of_the_students TEXT,
    amount_received TEXT,
    awarding_agency TEXT,
    awarding_organization TEXT,
    CONSTRAINT fk_scholarship_summary_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.scholarship_students (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    year TEXT,
    title_of_scholarship TEXT,
    name_of_the_students TEXT,
    amount_received TEXT,
    awarding_agency TEXT,
    CONSTRAINT fk_scholarship_students_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.faculty_information (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    cadre TEXT,
    required TEXT,
    regular TEXT,
    contract TEXT,
    CONSTRAINT fk_faculty_information_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.faculty_tenure (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    tenure TEXT,
    no_of_faculty TEXT,
    CONSTRAINT fk_faculty_tenure_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.faculty_experience (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    s_no TEXT,
    name_of_the_faculty TEXT,
    designation TEXT,
    highest_qualification TEXT,
    date_of_joining TEXT,
    experience_in_dypiu TEXT,
    experience_before_joining_dypiu TEXT,
    total_experience TEXT,
    CONSTRAINT fk_faculty_experience_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.supporting_staff (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    s_no TEXT,
    name_of_the_supporting_staff TEXT,
    designation TEXT,
    highest_qualification TEXT,
    date_of_joining_in_dypiu TEXT,
    experience_in_dypiu TEXT,
    experience_before_joining_dypiu TEXT,
    total_experience TEXT,
    CONSTRAINT fk_supporting_staff_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.staff_training (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    title_of_the_course TEXT,
    details_of_resource_person TEXT,
    duration_and_date_of_conduction TEXT,
    no_of_beneficiaries TEXT,
    CONSTRAINT fk_staff_training_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.building_infrastructure (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    facilities TEXT,
    no TEXT,
    CONSTRAINT fk_building_infrastructure_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.library_infrastructure (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    facilities TEXT,
    no TEXT,
    CONSTRAINT fk_library_infrastructure_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.e_resources (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    facilities TEXT,
    availability TEXT,
    remarks TEXT,
    CONSTRAINT fk_e_resources_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.it_infrastructure (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    facilities TEXT,
    no TEXT,
    CONSTRAINT fk_it_infrastructure_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.sports_facilities (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    facilities TEXT,
    no TEXT,
    CONSTRAINT fk_sports_facilities_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.divyangajan_facilities (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    facilities TEXT,
    available_yes_no TEXT,
    CONSTRAINT fk_divyangajan_facilities_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.research_resources (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    facilities TEXT,
    availability TEXT,
    remarks TEXT,
    CONSTRAINT fk_research_resources_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.hackathons (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    activity_details TEXT,
    organized_by TEXT,
    date_of_conduction TEXT,
    number_of_beneficiaries_participants TEXT,
    CONSTRAINT fk_hackathons_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.cultural_activities (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    activity_details TEXT,
    organized_by TEXT,
    date_of_conduction TEXT,
    number_of_beneficiaries_participants TEXT,
    CONSTRAINT fk_cultural_activities_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.sports_activities (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    activity_details TEXT,
    organized_by TEXT,
    date_of_conduction TEXT,
    number_of_beneficiaries_participants TEXT,
    CONSTRAINT fk_sports_activities_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.community_activities (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    activity_details TEXT,
    organized_by TEXT,
    date_of_conduction TEXT,
    number_of_beneficiaries_participants TEXT,
    CONSTRAINT fk_community_activities_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.admin_student_awards (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    name_of_the_award TEXT,
    team_individual TEXT,
    inter_university_state_national_international TEXT,
    name_of_the_event TEXT,
    name_of_the_student TEXT,
    CONSTRAINT fk_admin_student_awards_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.training_activities (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    academic_year TEXT,
    title_of_the_event TEXT,
    date_of_conduction TEXT,
    no_of_students_benefited TEXT,
    CONSTRAINT fk_training_activities_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.industry_collaborations (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    sr_no TEXT,
    name_of_the_organization_institution_industry_with_whom_mou_is_signed TEXT,
    year_of_signing_mou TEXT,
    duration_of_mou TEXT,
    list_the_actual_activities_under_each_mou TEXT,
    CONSTRAINT fk_industry_collaborations_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE public.password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
