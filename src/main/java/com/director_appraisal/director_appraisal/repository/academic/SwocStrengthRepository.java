package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.SwocStrength;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SwocStrengthRepository extends JpaRepository<SwocStrength, Long> {
    List<SwocStrength> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
