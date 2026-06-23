package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.ProfessionalBodies;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProfessionalBodiesRepository extends JpaRepository<ProfessionalBodies, Long> {
    List<ProfessionalBodies> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
