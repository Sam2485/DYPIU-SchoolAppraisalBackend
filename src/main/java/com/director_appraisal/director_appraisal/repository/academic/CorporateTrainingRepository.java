package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.CorporateTraining;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CorporateTrainingRepository extends JpaRepository<CorporateTraining, Long> {
    List<CorporateTraining> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
