package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.CareerGuidance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CareerGuidanceRepository extends JpaRepository<CareerGuidance, Long> {
    List<CareerGuidance> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
