package com.director_appraisal.director_appraisal.repository.administrative;

import com.director_appraisal.director_appraisal.model.administrative.ResearchResources;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResearchResourcesRepository extends JpaRepository<ResearchResources, Long> {
    List<ResearchResources> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
