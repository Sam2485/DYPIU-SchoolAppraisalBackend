package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.SyllabusRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SyllabusRevisionRepository extends JpaRepository<SyllabusRevision, Long> {
    List<SyllabusRevision> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
