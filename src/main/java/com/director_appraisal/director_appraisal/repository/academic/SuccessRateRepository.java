package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.SuccessRate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SuccessRateRepository extends JpaRepository<SuccessRate, Long> {
    List<SuccessRate> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
