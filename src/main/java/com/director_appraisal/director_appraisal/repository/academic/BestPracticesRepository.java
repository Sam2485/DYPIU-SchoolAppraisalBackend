package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.BestPractices;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BestPracticesRepository extends JpaRepository<BestPractices, Long> {
    List<BestPractices> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
