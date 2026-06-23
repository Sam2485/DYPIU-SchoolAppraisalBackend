package com.director_appraisal.director_appraisal.repository.administrative;

import com.director_appraisal.director_appraisal.model.administrative.FacultyExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FacultyExperienceRepository extends JpaRepository<FacultyExperience, Long> {
    List<FacultyExperience> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
