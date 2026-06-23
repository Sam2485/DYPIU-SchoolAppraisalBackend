package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.FunctionalMous;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FunctionalMousRepository extends JpaRepository<FunctionalMous, Long> {
    List<FunctionalMous> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
