package com.director_appraisal.director_appraisal.repository.administrative;

import com.director_appraisal.director_appraisal.model.administrative.ScholarshipSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScholarshipSummaryRepository extends JpaRepository<ScholarshipSummary, Long> {
    List<ScholarshipSummary> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
