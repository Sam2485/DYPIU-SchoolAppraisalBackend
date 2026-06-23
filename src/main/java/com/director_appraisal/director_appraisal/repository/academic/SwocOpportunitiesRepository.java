package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.SwocOpportunities;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SwocOpportunitiesRepository extends JpaRepository<SwocOpportunities, Long> {
    List<SwocOpportunities> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
