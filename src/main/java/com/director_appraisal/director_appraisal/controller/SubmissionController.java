package com.director_appraisal.director_appraisal.controller;

import com.director_appraisal.director_appraisal.model.Snapshot;
import com.director_appraisal.director_appraisal.model.Submission;
import com.director_appraisal.director_appraisal.model.User;
import com.director_appraisal.director_appraisal.service.SubmissionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@CrossOrigin
public class SubmissionController {

    private final SubmissionService submissionService;

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    private User getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new IllegalStateException("User not authenticated properly");
    }

    @GetMapping("/my-draft")
    public ResponseEntity<Submission> getMyDraft(@RequestParam String auditType) {
        String email = getCurrentUserEmail();
        Submission draft = submissionService.getOrCreateDraft(email, auditType.trim().toLowerCase());
        return ResponseEntity.ok(draft);
    }

    private void validateAuditTypeForRole(String role, String auditType) {
        String roleLower = role.toLowerCase();
        String typeLower = auditType.trim().toLowerCase();
        
        if ("director".equals(roleLower) && !"academic".equals(typeLower)) {
            throw new IllegalArgumentException("Academic Directors can only submit academic audits");
        }
        if ("administrative".equals(roleLower) && !"administrative".equals(typeLower)) {
            throw new IllegalArgumentException("Administrative users can only submit administrative audits");
        }
        if (List.of("vice-chancellor", "iqac").contains(roleLower)) {
            throw new IllegalArgumentException("Reviewers (VC & IQAC) cannot create or submit audits");
        }
    }

    @PostMapping("/save-draft")
    public ResponseEntity<Submission> saveDraft(@RequestBody FormSubmissionRequest request) {
        String email = getCurrentUserEmail();
        User user = getCurrentUserDetails();
        validateAuditTypeForRole(user.getRole(), request.getAuditType());
        Submission saved = submissionService.saveDraft(
                email,
                request.getAuditType().trim().toLowerCase(),
                user.getSchool(),
                user.getName(),
                request.getValuesData(),
                request.getTablesData(),
                request.getAttachments()
        );
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/submit")
    public ResponseEntity<Submission> submitForm(@RequestBody FormSubmissionRequest request) {
        String email = getCurrentUserEmail();
        User user = getCurrentUserDetails();
        validateAuditTypeForRole(user.getRole(), request.getAuditType());
        Submission submitted = submissionService.submitForm(
                email,
                request.getAuditType().trim().toLowerCase(),
                user.getSchool(),
                user.getName(),
                request.getValuesData(),
                request.getTablesData(),
                request.getAttachments()
        );
        return ResponseEntity.ok(submitted);
    }

    @PutMapping("/save-draft")
    public ResponseEntity<Submission> updateDraft(@RequestBody FormSubmissionRequest request) {
        return saveDraft(request);
    }

    @PutMapping("/submit")
    public ResponseEntity<Submission> updateAndSubmitForm(@RequestBody FormSubmissionRequest request) {
        return submitForm(request);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Submission> updateSubmission(@PathVariable Long id, @RequestBody FormSubmissionRequest request) {
        String email = getCurrentUserEmail();
        User user = getCurrentUserDetails();
        validateAuditTypeForRole(user.getRole(), request.getAuditType());
        Submission updated = submissionService.updateSubmissionById(
                id,
                email,
                user.getSchool(),
                user.getName(),
                request.getValuesData(),
                request.getTablesData(),
                request.getAttachments()
        );
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ROLE_VICE-CHANCELLOR', 'ROLE_IQAC')")
    public ResponseEntity<List<Submission>> getAllSubmissions() {
        List<Submission> submissions = submissionService.getSubmissionsForReviewer();
        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Submission> getSubmissionById(@PathVariable Long id) {
        // Anyone authenticated can view a form by ID (VC/IQAC to review, or owner)
        // Wait, for basic security we can check if caller is owner or is reviewer
        String email = getCurrentUserEmail();
        User user = getCurrentUserDetails();
        Submission submission = submissionService.getSubmissionById(id)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + id));

        boolean isOwner = submission.getEmail().equalsIgnoreCase(email);
        boolean isReviewer = List.of("vice-chancellor", "iqac").contains(user.getRole().toLowerCase());

        if (!isOwner && !isReviewer) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(submission);
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ROLE_VICE-CHANCELLOR', 'ROLE_IQAC')")
    public ResponseEntity<Submission> reviewSubmission(
            @PathVariable Long id,
            @RequestBody ReviewRequest request) {
        User reviewer = getCurrentUserDetails();
        Submission updated = submissionService.reviewSubmission(
                id,
                request.getStatus(),
                request.getRemarks(),
                reviewer.getName()
        );
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/snapshots")
    public ResponseEntity<List<Snapshot>> getSnapshots(@PathVariable Long id) {
        String email = getCurrentUserEmail();
        User user = getCurrentUserDetails();
        Submission submission = submissionService.getSubmissionById(id)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + id));

        boolean isOwner = submission.getEmail().equalsIgnoreCase(email);
        boolean isReviewer = List.of("vice-chancellor", "iqac").contains(user.getRole().toLowerCase());

        if (!isOwner && !isReviewer) {
            return ResponseEntity.status(403).build();
        }

        List<Snapshot> snapshots = submissionService.getSnapshotsForSubmission(id);
        return ResponseEntity.ok(snapshots);
    }

    @Data
    public static class FormSubmissionRequest {
        private String auditType;
        private String valuesData;
        private String tablesData;
        private String attachments;
    }

    @Data
    public static class ReviewRequest {
        private String status; // APPROVED, SENT_BACK, UNDER_REVIEW
        private String remarks;
    }
}
