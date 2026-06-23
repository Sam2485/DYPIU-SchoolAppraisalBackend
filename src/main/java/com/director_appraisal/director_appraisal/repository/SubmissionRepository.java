package com.director_appraisal.director_appraisal.repository;

import com.director_appraisal.director_appraisal.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    Optional<Submission> findByEmailAndAuditType(String email, String auditType);
    List<Submission> findByStatusIn(List<String> statuses);
    List<Submission> findByAuditTypeAndStatusIn(String auditType, List<String> statuses);
}
