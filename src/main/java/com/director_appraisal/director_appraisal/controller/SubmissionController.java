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
    public ResponseEntity<Submission> getMyDraft(@RequestParam(required = false) String auditType) {
        String email = getCurrentUserEmail();
        Submission draft = submissionService.getOrCreateDraft(email, normalizeAuditType(auditType));
        return ResponseEntity.ok(draft);
    }

    private String normalizeAuditType(String auditType) {
        if (auditType == null || auditType.isBlank() || "null".equalsIgnoreCase(auditType.trim())) {
            throw new IllegalArgumentException("Audit type is required");
        }
        return auditType.trim().toLowerCase();
    }

    private void validateAuditTypeForRole(String role, String auditType) {
        if (auditType == null) {
            return;
        }
        String roleLower = role.toLowerCase();
        String typeLower = auditType.trim().toLowerCase();
        
        if (roleLower.contains("auditor")) {
            if (roleLower.contains("academic") && !"academic".equals(typeLower)) {
                throw new IllegalArgumentException("Academic auditors can only audit academic forms");
            }
            if (roleLower.contains("administrative") && !"administrative".equals(typeLower)) {
                throw new IllegalArgumentException("Administrative auditors can only audit administrative forms");
            }
            return;
        }

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
        String auditType = normalizeAuditType(request.getAuditType());
        String email = getCurrentUserEmail();
        User user = getCurrentUserDetails();
        validateAuditTypeForRole(user.getRole(), auditType);
        Submission saved = submissionService.saveDraft(
                email,
                auditType,
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
        String auditType = normalizeAuditType(request.getAuditType());
        String email = getCurrentUserEmail();
        User user = getCurrentUserDetails();
        validateAuditTypeForRole(user.getRole(), auditType);
        Submission submitted = submissionService.submitForm(
                email,
                auditType,
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
        User user = getCurrentUserDetails();
        if (request.getAuditType() != null && !List.of("vice-chancellor", "iqac").contains(user.getRole().toLowerCase())) {
            validateAuditTypeForRole(user.getRole(), request.getAuditType());
        }
        Submission updated = submissionService.updateSubmission(
                id,
                user,
                request.getStatus(),
                request.getForwardedAuditorType(),
                request.getForwardedAuditCategory(),
                request.getForwardedToAuditorIds(),
                request.getForwardedToAuditorNames(),
                request.getForwardedToAuditorEmails(),
                request.getValuesData(),
                request.getTablesData(),
                request.getAttachments()
        );
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ROLE_VICE-CHANCELLOR', 'ROLE_IQAC', 'ROLE_ACADEMIC-INTERNAL-AUDITOR', 'ROLE_ACADEMIC-EXTERNAL-AUDITOR', 'ROLE_ADMINISTRATIVE-INTERNAL-AUDITOR', 'ROLE_ADMINISTRATIVE-EXTERNAL-AUDITOR')")
    public ResponseEntity<List<Submission>> getAllSubmissions() {
        User user = getCurrentUserDetails();
        List<Submission> submissions = submissionService.getAllSubmissionsForUser(user);
        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Submission> getSubmissionById(@PathVariable Long id) {
        String email = getCurrentUserEmail();
        User user = getCurrentUserDetails();
        Submission submission = submissionService.getSubmissionById(id)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + id));

        boolean isOwner = submission.getEmail().equalsIgnoreCase(email);
        boolean isIqac = "iqac".equalsIgnoreCase(user.getRole());
        boolean isVc = "vice-chancellor".equalsIgnoreCase(user.getRole());
        boolean isAuditor = user.getRole().toLowerCase().contains("auditor") || "auditor".equalsIgnoreCase(user.getAccountType());
        
        boolean isAssignedAuditor = isAuditor && (submissionService.isAuditorAssigned(user, submission) || submissionService.isAuditorFallbackMatch(user, submission));

        if (isVc) {
            boolean statusAllowed = List.of("AUDITOR_COMPLETED", "APPROVED", "FINAL").contains(submission.getStatus().toUpperCase());
            if (!statusAllowed) {
                return ResponseEntity.status(403).build();
            }
        }

        if (!isOwner && !isIqac && !isVc && !isAssignedAuditor) {
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
        boolean isIqac = "iqac".equalsIgnoreCase(user.getRole());
        boolean isVc = "vice-chancellor".equalsIgnoreCase(user.getRole());
        boolean isAuditor = user.getRole().toLowerCase().contains("auditor") || "auditor".equalsIgnoreCase(user.getAccountType());
        
        boolean isAssignedAuditor = isAuditor && (submissionService.isAuditorAssigned(user, submission) || submissionService.isAuditorFallbackMatch(user, submission));

        if (isVc) {
            boolean statusAllowed = List.of("AUDITOR_COMPLETED", "APPROVED", "FINAL").contains(submission.getStatus().toUpperCase());
            if (!statusAllowed) {
                return ResponseEntity.status(403).build();
            }
        }

        if (!isOwner && !isIqac && !isVc && !isAssignedAuditor) {
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
        private String status;
        private String forwardedAuditorType;
        private String forwardedAuditCategory;
        private List<Long> forwardedToAuditorIds;
        private List<String> forwardedToAuditorNames;
        private List<String> forwardedToAuditorEmails;
    }

    @Data
    public static class ReviewRequest {
        private String status; // APPROVED/FINAL, SENT_BACK, UNDER_REVIEW
        private String remarks;
    }
}
