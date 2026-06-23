package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.ResearchPublications;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResearchPublicationsRepository extends JpaRepository<ResearchPublications, Long> {
    List<ResearchPublications> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
