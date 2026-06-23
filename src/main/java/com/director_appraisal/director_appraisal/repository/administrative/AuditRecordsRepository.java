package com.director_appraisal.director_appraisal.repository.administrative;

import com.director_appraisal.director_appraisal.model.administrative.AuditRecords;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditRecordsRepository extends JpaRepository<AuditRecords, Long> {
    List<AuditRecords> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
