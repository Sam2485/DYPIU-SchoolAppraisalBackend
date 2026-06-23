package com.director_appraisal.director_appraisal.repository.administrative;

import com.director_appraisal.director_appraisal.model.administrative.StatutoryBodies;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StatutoryBodiesRepository extends JpaRepository<StatutoryBodies, Long> {
    List<StatutoryBodies> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
