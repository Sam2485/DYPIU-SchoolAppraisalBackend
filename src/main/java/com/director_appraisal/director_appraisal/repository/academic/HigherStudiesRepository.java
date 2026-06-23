package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.HigherStudies;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HigherStudiesRepository extends JpaRepository<HigherStudies, Long> {
    List<HigherStudies> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
