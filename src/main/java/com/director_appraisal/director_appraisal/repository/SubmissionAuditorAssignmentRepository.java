package com.director_appraisal.director_appraisal.repository;

import com.director_appraisal.director_appraisal.model.SubmissionAuditorAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionAuditorAssignmentRepository extends JpaRepository<SubmissionAuditorAssignment, Long> {
    List<SubmissionAuditorAssignment> findBySubmissionId(Long submissionId);
    boolean existsBySubmissionId(Long submissionId);
    boolean existsBySubmissionIdAndAuditorId(Long submissionId, Long auditorId);
    void deleteBySubmissionId(Long submissionId);
}
