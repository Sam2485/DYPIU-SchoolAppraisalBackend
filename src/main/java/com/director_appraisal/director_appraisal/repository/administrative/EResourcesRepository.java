package com.director_appraisal.director_appraisal.repository.administrative;

import com.director_appraisal.director_appraisal.model.administrative.EResources;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EResourcesRepository extends JpaRepository<EResources, Long> {
    List<EResources> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
