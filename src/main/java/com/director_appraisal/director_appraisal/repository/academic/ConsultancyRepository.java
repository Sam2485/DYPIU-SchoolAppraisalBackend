package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.Consultancy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConsultancyRepository extends JpaRepository<Consultancy, Long> {
    List<Consultancy> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
