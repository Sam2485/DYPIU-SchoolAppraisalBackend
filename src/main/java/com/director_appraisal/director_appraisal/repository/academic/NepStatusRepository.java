package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.NepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NepStatusRepository extends JpaRepository<NepStatus, Long> {
    List<NepStatus> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
