-- Core tables for appraisal application

CREATE TABLE IF NOT EXISTS public.users (
                              id BIGSERIAL PRIMARY KEY,
                              email VARCHAR(255) NOT NULL UNIQUE,
                              password VARCHAR(255) NOT NULL,
                              name VARCHAR(255),
                              designation VARCHAR(255),
                              school VARCHAR(255),
                              role VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS public.submissions (
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

CREATE TABLE IF NOT EXISTS public.snapshots (
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

CREATE TABLE IF NOT EXISTS public.student_strength (
                                         id BIGSERIAL PRIMARY KEY,
                                         submission_id BIGINT NOT NULL,
                                         class_name TEXT,
                                         no_of_students TEXT,
                                         total TEXT,
                                         CONSTRAINT fk_student_strength_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.faculty_strength (
                                         id BIGSERIAL PRIMARY KEY,
                                         submission_id BIGINT NOT NULL,
                                         required_faculty TEXT,
                                         available TEXT,
                                         CONSTRAINT fk_faculty_strength_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.board_of_studies (
                                         id BIGSERIAL PRIMARY KEY,
                                         submission_id BIGINT NOT NULL,
                                         sr_no TEXT,
                                         meeting_date TEXT,
                                         link_for_mom TEXT,
                                         CONSTRAINT fk_board_of_studies_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.syllabus_revision (
                                          id BIGSERIAL PRIMARY KEY,
                                          submission_id BIGINT NOT NULL,
                                          sr_no TEXT,
                                          category_of_feedback TEXT,
                                          link_analysis_atr TEXT,
                                          CONSTRAINT fk_syllabus_revision_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.obe_implementation (
                                           id BIGSERIAL PRIMARY KEY,
                                           submission_id BIGINT NOT NULL,
                                           sr_no TEXT,
                                           particular TEXT,
                                           link_document TEXT,
                                           CONSTRAINT fk_obe_implementation_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.nep_status (
                                   id BIGSERIAL PRIMARY KEY,
                                   submission_id BIGINT NOT NULL,
                                   sn TEXT,
                                   check_points TEXT,
                                   availability TEXT,
                                   link_document TEXT,
                                   CONSTRAINT fk_nep_status_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.best_practices (
                                       id BIGSERIAL PRIMARY KEY,
                                       submission_id BIGINT NOT NULL,
                                       sn TEXT,
                                       check_points TEXT,
                                       availability TEXT,
                                       link_document TEXT,
                                       CONSTRAINT fk_best_practices_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.student_mentoring (
                                          id BIGSERIAL PRIMARY KEY,
                                          submission_id BIGINT NOT NULL,
                                          sr_no TEXT,
                                          mentor_name TEXT,
                                          no_of_mentees TEXT,
                                          link_to_document TEXT,
                                          CONSTRAINT fk_student_mentoring_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.graduating_students (
                                            id BIGSERIAL PRIMARY KEY,
                                            submission_id BIGINT NOT NULL,
                                            program TEXT,
                                            female TEXT,
                                            male TEXT,
                                            total TEXT,
                                            CONSTRAINT fk_graduating_students_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.success_rate (
                                     id BIGSERIAL PRIMARY KEY,
                                     submission_id BIGINT NOT NULL,
                                     program TEXT,
                                     students_appeared TEXT,
                                     students_cleared TEXT,
                                     success_rate_percent TEXT,
                                     CONSTRAINT fk_success_rate_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.qualifying_exams (
                                         id BIGSERIAL PRIMARY KEY,
                                         submission_id BIGINT NOT NULL,
                                         sr_no TEXT,
                                         student_name TEXT,
                                         examination_details TEXT,
                                         proof_attachment TEXT,
                                         CONSTRAINT fk_qualifying_exams_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.student_awards (
                                       id BIGSERIAL PRIMARY KEY,
                                       submission_id BIGINT NOT NULL,
                                       sr_no TEXT,
                                       student_name TEXT,
                                       award_details TEXT,
                                       proof_attachment TEXT,
                                       CONSTRAINT fk_student_awards_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.student_placements (
                                           id BIGSERIAL PRIMARY KEY,
                                           submission_id BIGINT NOT NULL,
                                           program TEXT,
                                           students_appeared TEXT,
                                           students_placed TEXT,
                                           placement_percent TEXT,
                                           proof_attachment TEXT,
                                           CONSTRAINT fk_student_placements_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.higher_studies (
                                       id BIGSERIAL PRIMARY KEY,
                                       submission_id BIGINT NOT NULL,
                                       program TEXT,
                                       students_appeared TEXT,
                                       selected_students TEXT,
                                       students_percent TEXT,
                                       CONSTRAINT fk_higher_studies_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.student_startups (
                                         id BIGSERIAL PRIMARY KEY,
                                         submission_id BIGINT NOT NULL,
                                         sn TEXT,
                                         student_name TEXT,
                                         venture_name TEXT,
                                         link_proof TEXT,
                                         CONSTRAINT fk_student_startups_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.student_courses (
                                        id BIGSERIAL PRIMARY KEY,
                                        submission_id BIGINT NOT NULL,
                                        sr_no TEXT,
                                        name_of_student TEXT,
                                        year_of_study TEXT,
                                        name_of_course TEXT,
                                        duration TEXT,
                                        link_proof TEXT,
                                        CONSTRAINT fk_student_courses_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.alumni_interactions (
                                            id BIGSERIAL PRIMARY KEY,
                                            submission_id BIGINT NOT NULL,
                                            sr_no TEXT,
                                            alumni_name TEXT,
                                            designation TEXT,
                                            present_employer TEXT,
                                            interaction_date TEXT,
                                            topic TEXT,
                                            no_of_beneficiaries TEXT,
                                            link_proof TEXT,
                                            CONSTRAINT fk_alumni_interactions_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.guest_lectures (
                                       id BIGSERIAL PRIMARY KEY,
                                       submission_id BIGINT NOT NULL,
                                       sr_no TEXT,
                                       resource_person TEXT,
                                       designation_org TEXT,
                                       conduction_date TEXT,
                                       topic TEXT,
                                       no_beneficiaries TEXT,
                                       link_proof TEXT,
                                       CONSTRAINT fk_guest_lectures_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.professional_bodies (
                                            id BIGSERIAL PRIMARY KEY,
                                            submission_id BIGINT NOT NULL,
                                            sr_no TEXT,
                                            body_name TEXT,
                                            student_members TEXT,
                                            event_date TEXT,
                                            event_name TEXT,
                                            link_proof TEXT,
                                            CONSTRAINT fk_professional_bodies_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.value_added_courses (
                                            id BIGSERIAL PRIMARY KEY,
                                            submission_id BIGINT NOT NULL,
                                            sr_no TEXT,
                                            course_title TEXT,
                                            resource_person TEXT,
                                            duration_date TEXT,
                                            no_of_beneficiaries TEXT,
                                            link_proof TEXT,
                                            CONSTRAINT fk_value_added_courses_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.career_guidance (
                                        id BIGSERIAL PRIMARY KEY,
                                        submission_id BIGINT NOT NULL,
                                        sr_no TEXT,
                                        session_details TEXT,
                                        resource_person TEXT,
                                        conduction_date TEXT,
                                        no_beneficiaries TEXT,
                                        link_proof TEXT,
                                        CONSTRAINT fk_career_guidance_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.extension_activities (
                                             id BIGSERIAL PRIMARY KEY,
                                             submission_id BIGINT NOT NULL,
                                             sr_no TEXT,
                                             activity_details TEXT,
                                             organized_by TEXT,
                                             conduction_date TEXT,
                                             no_beneficiaries TEXT,
                                             link_proof TEXT,
                                             CONSTRAINT fk_extension_activities_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.faculty_specialization (
                                               id BIGSERIAL PRIMARY KEY,
                                               submission_id BIGINT NOT NULL,
                                               sr_no TEXT,
                                               name TEXT,
                                               designation TEXT,
                                               qualifications TEXT,
                                               specialization TEXT,
                                               phd_supervised TEXT,
                                               CONSTRAINT fk_faculty_specialization_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.research_publications (
                                              id BIGSERIAL PRIMARY KEY,
                                              submission_id BIGINT NOT NULL,
                                              paper_title TEXT,
                                              author_name TEXT,
                                              journal_name TEXT,
                                              publication_details TEXT,
                                              isbn_issn TEXT,
                                              ugc_approved TEXT,
                                              journal_type TEXT,
                                              impact_factor TEXT,
                                              link_proof TEXT,
                                              CONSTRAINT fk_research_publications_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.books_chapters (
                                       id BIGSERIAL PRIMARY KEY,
                                       submission_id BIGINT NOT NULL,
                                       teacher_name TEXT,
                                       book_chapters_title TEXT,
                                       paper_title TEXT,
                                       proceedings_title TEXT,
                                       conference_name TEXT,
                                       scope TEXT,
                                       publication_year TEXT,
                                       isbn_issn TEXT,
                                       publisher_name TEXT,
                                       link_proof TEXT,
                                       CONSTRAINT fk_books_chapters_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.corporate_training (
                                           id BIGSERIAL PRIMARY KEY,
                                           submission_id BIGINT NOT NULL,
                                           sr_no TEXT,
                                           faculty_name TEXT,
                                           training_agency TEXT,
                                           revenue_generated TEXT,
                                           number_of_trainees TEXT,
                                           link_proof TEXT,
                                           CONSTRAINT fk_corporate_training_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.consultancy (
                                    id BIGSERIAL PRIMARY KEY,
                                    submission_id BIGINT NOT NULL,
                                    faculty_name TEXT,
                                    project_title TEXT,
                                    sponsoring_agency TEXT,
                                    revenue_generated TEXT,
                                    link_proof TEXT,
                                    CONSTRAINT fk_consultancy_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.research_funds (
                                       id BIGSERIAL PRIMARY KEY,
                                       submission_id BIGINT NOT NULL,
                                       sr_no TEXT,
                                       project_name TEXT,
                                       principal_investigator TEXT,
                                       department_pi TEXT,
                                       year_of_award TEXT,
                                       funds_provided TEXT,
                                       project_duration TEXT,
                                       link_proof TEXT,
                                       CONSTRAINT fk_research_funds_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.e_contents (
                                   id BIGSERIAL PRIMARY KEY,
                                   submission_id BIGINT NOT NULL,
                                   sr_no TEXT,
                                   teacher_name TEXT,
                                   module_name TEXT,
                                   platform TEXT,
                                   launch_date TEXT,
                                   link_proof TEXT,
                                   CONSTRAINT fk_e_contents_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.teacher_awards (
                                       id BIGSERIAL PRIMARY KEY,
                                       submission_id BIGINT NOT NULL,
                                       teacher_name TEXT,
                                       national_awards TEXT,
                                       international_awards TEXT,
                                       link_proof TEXT,
                                       CONSTRAINT fk_teacher_awards_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.patents_copyrights (
                                           id BIGSERIAL PRIMARY KEY,
                                           submission_id BIGINT NOT NULL,
                                           sr_no TEXT,
                                           inventor_name TEXT,
                                           application_no TEXT,
                                           title TEXT,
                                           date_of_filing TEXT,
                                           date_of_publication TEXT,
                                           date_of_award TEXT,
                                           link_proof TEXT,
                                           CONSTRAINT fk_patents_copyrights_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.fdp_organized (
                                      id BIGSERIAL PRIMARY KEY,
                                      submission_id BIGINT NOT NULL,
                                      sl_no TEXT,
                                      coordinator TEXT,
                                      seminar_title TEXT,
                                      sponsoring_agency TEXT,
                                      duration_dates TEXT,
                                      participants_count TEXT,
                                      published TEXT,
                                      link_proof TEXT,
                                      CONSTRAINT fk_fdp_organized_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.fdp_attended (
                                     id BIGSERIAL PRIMARY KEY,
                                     submission_id BIGINT NOT NULL,
                                     sl_no TEXT,
                                     faculty_name TEXT,
                                     seminar_title TEXT,
                                     sponsoring_org TEXT,
                                     duration_dates TEXT,
                                     date TEXT,
                                     link_proof TEXT,
                                     CONSTRAINT fk_fdp_attended_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.functional_mous (
                                        id BIGSERIAL PRIMARY KEY,
                                        submission_id BIGINT NOT NULL,
                                        sr_no TEXT,
                                        partner_org TEXT,
                                        signing_year TEXT,
                                        mou_duration TEXT,
                                        activities TEXT,
                                        link_proof TEXT,
                                        CONSTRAINT fk_functional_mous_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.swoc_strength (
                                      id BIGSERIAL PRIMARY KEY,
                                      submission_id BIGINT NOT NULL,
                                      sr_no TEXT,
                                      details TEXT,
                                      CONSTRAINT fk_swoc_strength_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.swoc_weaknesses (
                                        id BIGSERIAL PRIMARY KEY,
                                        submission_id BIGINT NOT NULL,
                                        sr_no TEXT,
                                        details TEXT,
                                        CONSTRAINT fk_swoc_weaknesses_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.swoc_opportunities (
                                           id BIGSERIAL PRIMARY KEY,
                                           submission_id BIGINT NOT NULL,
                                           sr_no TEXT,
                                           details TEXT,
                                           CONSTRAINT fk_swoc_opportunities_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.swoc_challenges (
                                        id BIGSERIAL PRIMARY KEY,
                                        submission_id BIGINT NOT NULL,
                                        sr_no TEXT,
                                        details TEXT,
                                        CONSTRAINT fk_swoc_challenges_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.swoc_other_information (
                                               id BIGSERIAL PRIMARY KEY,
                                               submission_id BIGINT NOT NULL,
                                               sr_no TEXT,
                                               details TEXT,
                                               CONSTRAINT fk_swoc_other_information_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.courses_offered (
                                        id BIGSERIAL PRIMARY KEY,
                                        submission_id BIGINT NOT NULL,
                                        sr_no TEXT,
                                        program_name TEXT,
                                        level_ug_pg TEXT,
                                        intake TEXT,
                                        commencement_year TEXT,
                                        CONSTRAINT fk_courses_offered_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.student_statistics (
                                           id BIGSERIAL PRIMARY KEY,
                                           submission_id BIGINT NOT NULL,
                                           sr_no TEXT,
                                           category TEXT,
                                           ug TEXT,
                                           pg TEXT,
                                           phd TEXT,
                                           skill_courses TEXT,
                                           CONSTRAINT fk_student_statistics_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.statutory_bodies (
                                         id BIGSERIAL PRIMARY KEY,
                                         submission_id BIGINT NOT NULL,
                                         sr_no TEXT,
                                         body_cell TEXT,
                                         meetings_conducted TEXT,
                                         atr_status TEXT,
                                         remarks_link TEXT,
                                         CONSTRAINT fk_statutory_bodies_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.audit_records (
                                      id BIGSERIAL PRIMARY KEY,
                                      submission_id BIGINT NOT NULL,
                                      sr_no TEXT,
                                      audit_type TEXT,
                                      completed_yes_no TEXT,
                                      date TEXT,
                                      remarks_link TEXT,
                                      CONSTRAINT fk_audit_records_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.scholarship_summary (
                                            id BIGSERIAL PRIMARY KEY,
                                            submission_id BIGINT NOT NULL,
                                            sr_no TEXT,
                                            year TEXT,
                                            scholarship_title TEXT,
                                            students_count TEXT,
                                            amount_received TEXT,
                                            awarding_agency TEXT,
                                            awarding_org TEXT,
                                            CONSTRAINT fk_scholarship_summary_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.scholarship_students (
                                             id BIGSERIAL PRIMARY KEY,
                                             submission_id BIGINT NOT NULL,
                                             sr_no TEXT,
                                             year TEXT,
                                             scholarship_title TEXT,
                                             student_name TEXT,
                                             amount_received TEXT,
                                             awarding_agency TEXT,
                                             CONSTRAINT fk_scholarship_students_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.faculty_information (
                                            id BIGSERIAL PRIMARY KEY,
                                            submission_id BIGINT NOT NULL,
                                            sr_no TEXT,
                                            cadre TEXT,
                                            required TEXT,
                                            regular TEXT,
                                            contract TEXT,
                                            CONSTRAINT fk_faculty_information_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.faculty_tenure (
                                       id BIGSERIAL PRIMARY KEY,
                                       submission_id BIGINT NOT NULL,
                                       sr_no TEXT,
                                       tenure TEXT,
                                       no_of_faculty TEXT,
                                       CONSTRAINT fk_faculty_tenure_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.faculty_experience (
                                           id BIGSERIAL PRIMARY KEY,
                                           submission_id BIGINT NOT NULL,
                                           s_no TEXT,
                                           faculty_name TEXT,
                                           designation TEXT,
                                           qualification TEXT,
                                           joining_date TEXT,
                                           experience_dypiu TEXT,
                                           prior_experience TEXT,
                                           total_experience TEXT,
                                           CONSTRAINT fk_faculty_experience_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.supporting_staff (
                                         id BIGSERIAL PRIMARY KEY,
                                         submission_id BIGINT NOT NULL,
                                         s_no TEXT,
                                         staff_name TEXT,
                                         designation TEXT,
                                         qualification TEXT,
                                         joining_date TEXT,
                                         experience_dypiu TEXT,
                                         prior_experience TEXT,
                                         total_experience TEXT,
                                         CONSTRAINT fk_supporting_staff_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.staff_training (
                                       id BIGSERIAL PRIMARY KEY,
                                       submission_id BIGINT NOT NULL,
                                       sr_no TEXT,
                                       course_title TEXT,
                                       resource_person TEXT,
                                       duration_date TEXT,
                                       no_of_beneficiaries TEXT,
                                       CONSTRAINT fk_staff_training_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.building_infrastructure (
                                                id BIGSERIAL PRIMARY KEY,
                                                submission_id BIGINT NOT NULL,
                                                sr_no TEXT,
                                                facilities TEXT,
                                                no TEXT,
                                                CONSTRAINT fk_building_infrastructure_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.library_infrastructure (
                                               id BIGSERIAL PRIMARY KEY,
                                               submission_id BIGINT NOT NULL,
                                               sr_no TEXT,
                                               facilities TEXT,
                                               no TEXT,
                                               CONSTRAINT fk_library_infrastructure_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.e_resources (
                                    id BIGSERIAL PRIMARY KEY,
                                    submission_id BIGINT NOT NULL,
                                    sr_no TEXT,
                                    facilities TEXT,
                                    availability TEXT,
                                    remarks TEXT,
                                    CONSTRAINT fk_e_resources_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.it_infrastructure (
                                          id BIGSERIAL PRIMARY KEY,
                                          submission_id BIGINT NOT NULL,
                                          sr_no TEXT,
                                          facilities TEXT,
                                          no TEXT,
                                          CONSTRAINT fk_it_infrastructure_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.sports_facilities (
                                          id BIGSERIAL PRIMARY KEY,
                                          submission_id BIGINT NOT NULL,
                                          sr_no TEXT,
                                          facilities TEXT,
                                          no TEXT,
                                          CONSTRAINT fk_sports_facilities_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.divyangajan_facilities (
                                               id BIGSERIAL PRIMARY KEY,
                                               submission_id BIGINT NOT NULL,
                                               sr_no TEXT,
                                               facilities TEXT,
                                               available_yes_no TEXT,
                                               CONSTRAINT fk_divyangajan_facilities_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.research_resources (
                                           id BIGSERIAL PRIMARY KEY,
                                           submission_id BIGINT NOT NULL,
                                           sr_no TEXT,
                                           facilities TEXT,
                                           availability TEXT,
                                           remarks TEXT,
                                           CONSTRAINT fk_research_resources_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.hackathons (
                                   id BIGSERIAL PRIMARY KEY,
                                   submission_id BIGINT NOT NULL,
                                   sr_no TEXT,
                                   activity_details TEXT,
                                   organized_by TEXT,
                                   conduction_date TEXT,
                                   participants_count TEXT,
                                   CONSTRAINT fk_hackathons_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.cultural_activities (
                                            id BIGSERIAL PRIMARY KEY,
                                            submission_id BIGINT NOT NULL,
                                            sr_no TEXT,
                                            activity_details TEXT,
                                            organized_by TEXT,
                                            conduction_date TEXT,
                                            participants_count TEXT,
                                            CONSTRAINT fk_cultural_activities_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.sports_activities (
                                          id BIGSERIAL PRIMARY KEY,
                                          submission_id BIGINT NOT NULL,
                                          sr_no TEXT,
                                          activity_details TEXT,
                                          organized_by TEXT,
                                          conduction_date TEXT,
                                          participants_count TEXT,
                                          CONSTRAINT fk_sports_activities_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.community_activities (
                                             id BIGSERIAL PRIMARY KEY,
                                             submission_id BIGINT NOT NULL,
                                             sr_no TEXT,
                                             activity_details TEXT,
                                             organized_by TEXT,
                                             conduction_date TEXT,
                                             participants_count TEXT,
                                             CONSTRAINT fk_community_activities_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.admin_student_awards (
                                             id BIGSERIAL PRIMARY KEY,
                                             submission_id BIGINT NOT NULL,
                                             sr_no TEXT,
                                             award_name TEXT,
                                             team_individual TEXT,
                                             level_type TEXT,
                                             event_name TEXT,
                                             student_name TEXT,
                                             CONSTRAINT fk_admin_student_awards_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.training_activities (
                                            id BIGSERIAL PRIMARY KEY,
                                            submission_id BIGINT NOT NULL,
                                            sr_no TEXT,
                                            academic_year TEXT,
                                            event_name TEXT,
                                            conduction_date TEXT,
                                            students_benefited TEXT,
                                            CONSTRAINT fk_training_activities_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.industry_collaborations (
                                                id BIGSERIAL PRIMARY KEY,
                                                submission_id BIGINT NOT NULL,
                                                sr_no TEXT,
                                                partner_org TEXT,
                                                signing_year TEXT,
                                                mou_duration TEXT,
                                                activities TEXT,
                                                CONSTRAINT fk_industry_collaborations_submission FOREIGN KEY (submission_id) REFERENCES public.submissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.password_reset_tokens (
                                              id BIGSERIAL PRIMARY KEY,
                                              email VARCHAR(255) NOT NULL,
                                              token_hash VARCHAR(255) NOT NULL UNIQUE,
                                              used BOOLEAN NOT NULL DEFAULT FALSE,
                                              expires_at TIMESTAMP NOT NULL,
                                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
