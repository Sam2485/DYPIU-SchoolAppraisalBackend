package com.director_appraisal.director_appraisal.service;

import com.director_appraisal.director_appraisal.model.Snapshot;
import com.director_appraisal.director_appraisal.model.Submission;
import com.director_appraisal.director_appraisal.repository.SnapshotRepository;
import com.director_appraisal.director_appraisal.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final SnapshotRepository snapshotRepository;

    @Transactional
    public Submission getOrCreateDraft(String email, String auditType) {
        return submissionRepository.findByEmailAndAuditType(email, auditType)
                .orElseGet(() -> {
                    Submission submission = Submission.builder()
                            .email(email)
                            .auditType(auditType)
                            .status("DRAFT")
                            .valuesData("{}")
                            .tablesData("{}")
                            .attachments("[]")
                            .version(1)
                            .build();
                    return submissionRepository.save(submission);
                });
    }

    @Transactional
    public Submission saveDraft(String email, String auditType, String school, String submittedBy, 
                                 String valuesData, String tablesData, String attachments) {
        Submission submission = getOrCreateDraft(email, auditType);
        
        // Updates can only occur before final approval or during draft/sent-back status
        if ("APPROVED".equalsIgnoreCase(submission.getStatus())) {
            throw new IllegalStateException("Cannot edit an approved submission");
        }

        submission.setSchool(school);
        submission.setSubmittedBy(submittedBy);
        submission.setValuesData(valuesData);
        submission.setTablesData(tablesData);
        submission.setAttachments(attachments);
        submission.setVersion(submission.getVersion() + 1);

        Submission saved = submissionRepository.save(submission);
        createSnapshot(saved);
        return saved;
    }

    @Transactional
    public Submission submitForm(String email, String auditType, String school, String submittedBy, 
                                 String valuesData, String tablesData, String attachments) {
        Submission submission = getOrCreateDraft(email, auditType);

        if ("APPROVED".equalsIgnoreCase(submission.getStatus())) {
            throw new IllegalStateException("Cannot resubmit an approved submission");
        }

        submission.setSchool(school);
        submission.setSubmittedBy(submittedBy);
        submission.setValuesData(valuesData);
        submission.setTablesData(tablesData);
        submission.setAttachments(attachments);
        submission.setStatus("SUBMITTED");
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setVersion(submission.getVersion() + 1);

        Submission saved = submissionRepository.save(submission);
        createSnapshot(saved);
        return saved;
    }

    public List<Submission> getSubmissionsForReviewer() {
        // VC & IQAC only view submitted, under-review, approved, and sent-back forms
        return submissionRepository.findByStatusIn(List.of("SUBMITTED", "UNDER_REVIEW", "APPROVED", "SENT_BACK"));
    }

    public Optional<Submission> getSubmissionById(Long id) {
        return submissionRepository.findById(id);
    }

    @Transactional
    public Submission reviewSubmission(Long id, String status, String remarks, String reviewerName) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + id));

        if (!List.of("APPROVED", "SENT_BACK", "UNDER_REVIEW").contains(status.toUpperCase())) {
            throw new IllegalArgumentException("Invalid review status: " + status);
        }

        submission.setStatus(status.toUpperCase());
        submission.setRemarks(remarks);
        submission.setReviewedBy(reviewerName);
        submission.setReviewedAt(LocalDateTime.now());
        submission.setVersion(submission.getVersion() + 1);

        Submission saved = submissionRepository.save(submission);
        createSnapshot(saved);
        return saved;
    }

    public List<Snapshot> getSnapshotsForSubmission(Long submissionId) {
        return snapshotRepository.findBySubmissionIdOrderByVersionDesc(submissionId);
    }

    private void createSnapshot(Submission submission) {
        Snapshot snapshot = Snapshot.builder()
                .submissionId(submission.getId())
                .savedAt(LocalDateTime.now())
                .status(submission.getStatus())
                .valuesData(submission.getValuesData())
                .tablesData(submission.getTablesData())
                .attachments(submission.getAttachments())
                .version(submission.getVersion())
                .build();
        snapshotRepository.save(snapshot);
    }
}
