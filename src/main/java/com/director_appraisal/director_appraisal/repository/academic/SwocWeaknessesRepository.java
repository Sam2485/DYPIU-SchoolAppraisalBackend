package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.SwocWeaknesses;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SwocWeaknessesRepository extends JpaRepository<SwocWeaknesses, Long> {
    List<SwocWeaknesses> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
