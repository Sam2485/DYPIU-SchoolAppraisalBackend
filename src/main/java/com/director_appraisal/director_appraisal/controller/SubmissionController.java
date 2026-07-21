package com.director_appraisal.director_appraisal.controller;

import com.director_appraisal.director_appraisal.model.Submission;
import com.director_appraisal.director_appraisal.model.User;
import com.director_appraisal.director_appraisal.service.SubmissionService;
import com.director_appraisal.director_appraisal.service.AttachmentService;
import com.director_appraisal.director_appraisal.util.SchoolUtils;
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
    private final AttachmentService attachmentService;
    private final com.director_appraisal.director_appraisal.repository.UserRepository userRepository;

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
    public ResponseEntity<Submission> getMyDraft(@RequestParam(required = false) String auditType,
                                                 @RequestParam(required = false, defaultValue = "false") boolean shared) {
        String email = getCurrentUserEmail();
        User user = getCurrentUserDetails();
        String normalizedAuditType = normalizeAuditType(auditType);
        Submission draft = shared && "administrative".equals(normalizedAuditType)
                ? submissionService.getOrCreateSharedAdministrativeDraft(user)
                : submissionService.getOrCreateDraft(email, normalizedAuditType);
        submissionService.populateAuditorProgressAndAssignments(draft);
        return ResponseEntity.ok(draft);
    }

    @GetMapping("/administrative/{cycleId}/status")
    public ResponseEntity<Object> getAdministrativeStatus(@PathVariable String cycleId) {
        Submission submission = submissionService.getOrCreateSharedAdministrativeDraftForCycle(cycleId);
        return ResponseEntity.ok(submission.getSubmittedByForJson());
    }

    @PostMapping("/administrative/{cycleId}/submit")
    public ResponseEntity<Submission> submitAdministrativePart(@PathVariable String cycleId) {
        User caller = getCurrentUserDetails();
        Submission submitted = submissionService.submitAdministrativePart(cycleId, caller);
        return ResponseEntity.ok(submitted);
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
        if (request.isSharedAdministrativeForm() && "administrative".equals(auditType)) {
            return ResponseEntity.ok(submissionService.saveSharedAdministrativeContribution(user, request.getContributorPost(),
                    request.getSections(), request.getValuesData(), request.getTablesData(), request.getAttachments(), false));
        }
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
        if (request.isSharedAdministrativeForm() && "administrative".equals(auditType)) {
            return ResponseEntity.ok(submissionService.saveSharedAdministrativeContribution(user, request.getContributorPost(),
                    request.getSections(), request.getValuesData(), request.getTablesData(), request.getAttachments(), true));
        }
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
        if (request.isSharedAdministrativeForm() && "administrative".equalsIgnoreCase(request.getAuditType())) {
            Submission updated = submissionService.updateSharedAdministrativeContribution(
                    id,
                    user,
                    request.getAction(),
                    request.getContributorPost(),
                    request.getSections(),
                    request.getValuesData(),
                    request.getTablesData(),
                    request.getAttachments()
            );
            return ResponseEntity.ok(updated);
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
                request.getAttachments(),
                request.getForwardedAdministrativePosts(),
                request.getForwardedToAuditorPosts(),
                request.getAuditorCorrectionRequested(),
                request.getCorrectionRequestedForAuditor(),
                request.getRequiresAuditorResubmission(),
                request.getAuditorCorrectionMessage(),
                request.getAuditorCorrectionRequestedBy(),
                request.getAuditorCorrectionRequestedByRole(),
                request.getAuditorCorrectionRequestedOn(),
                request.getAuditorResubmittedAt(),
                request.getRemarks()
        );
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/auditor-submit")
    @PreAuthorize("hasAnyRole('ROLE_ACADEMIC-INTERNAL-AUDITOR', 'ROLE_ACADEMIC-EXTERNAL-AUDITOR', 'ROLE_ADMINISTRATIVE-INTERNAL-AUDITOR', 'ROLE_ADMINISTRATIVE-EXTERNAL-AUDITOR')")
    public ResponseEntity<?> submitAuditorReview(@PathVariable Long id, @RequestBody AuditorSubmitRequest request) {
        User user = getCurrentUserDetails();
        Object response = submissionService.submitAuditorReview(id, user, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ROLE_VICE-CHANCELLOR', 'ROLE_IQAC', 'ROLE_ACADEMIC-INTERNAL-AUDITOR', 'ROLE_ACADEMIC-EXTERNAL-AUDITOR', 'ROLE_ADMINISTRATIVE-INTERNAL-AUDITOR', 'ROLE_ADMINISTRATIVE-EXTERNAL-AUDITOR')")
    public ResponseEntity<List<Submission>> getAllSubmissions() {
        User user = getCurrentUserDetails();
        List<Submission> submissions = submissionService.getAllSubmissionsForUser(user);
        submissions.forEach(sub -> submissionService.populatePermissions(sub, user));
        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/previous-reports")
    @PreAuthorize("hasAnyRole('ROLE_VICE-CHANCELLOR', 'ROLE_IQAC')")
    public ResponseEntity<List<Submission>> getPreviousReports(@RequestParam(required = false) String academicYear) {
        return ResponseEntity.ok(submissionService.getPreviousReports(getCurrentUserDetails(), academicYear));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Submission> getSubmissionById(@PathVariable Long id) {
        String email = getCurrentUserEmail();
        User user = getCurrentUserDetails();
        Submission submission = submissionService.getSubmissionById(id)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + id));

        if (!"APPROVED".equalsIgnoreCase(submission.getStatus()) && !"FINAL".equalsIgnoreCase(submission.getStatus())) {
            if (submission.getEmail() != null) {
                java.util.Optional<User> submitter = userRepository.findByEmail(submission.getEmail().trim().toLowerCase());
                if (submitter.isPresent() && Boolean.TRUE.equals(submitter.get().getDeleted())) {
                    throw new IllegalArgumentException("Submission not found with ID: " + id);
                }
            }
        }

        boolean isOwner = submission.getEmail().equalsIgnoreCase(email);
        boolean isIqac = "iqac".equalsIgnoreCase(user.getRole());
        boolean isVc = "vice-chancellor".equalsIgnoreCase(user.getRole());
        boolean isAuditor = user.getRole().toLowerCase().contains("auditor") || "auditor".equalsIgnoreCase(user.getAccountType());
        boolean isAdministrativeContributor = "administrative".equalsIgnoreCase(user.getRole())
                && "administrative".equalsIgnoreCase(submission.getAuditType());
        
        boolean isAssignedAuditor = isAuditor && (submissionService.isAuditorAssigned(user, submission) || submissionService.isAuditorFallbackMatch(user, submission));

        if (isVc) {
            boolean statusAllowed = List.of("AUDITOR_COMPLETED", "APPROVED", "FINAL").contains(submission.getStatus().toUpperCase());
            if (!statusAllowed) {
                return ResponseEntity.status(403).build();
            }
        }

        if (!isOwner && !isIqac && !isVc && !isAssignedAuditor && !isAdministrativeContributor) {
            return ResponseEntity.status(403).build();
        }

        submissionService.populatePermissions(submission, user);
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
                request.getReportCategory(),
                request.getAuditCycle(),
                request.getVersion(),
                request.getValuesData(),
                request.getTablesData(),
                request.getAttachments(),
                reviewer
        );
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/next-cycle")
    @PreAuthorize("hasAnyRole('ROLE_VICE-CHANCELLOR', 'ROLE_IQAC')")
    public ResponseEntity<Submission> createNextCycle(
            @PathVariable Long id,
            @RequestBody NextCycleRequest request) {
        User caller = getCurrentUserDetails();
        Submission nextSubmission = submissionService.createNextCycle(
                id,
                caller,
                request.isPreserveApprovedVersion(),
                request.getPreviousApprovedSubmissionId(),
                request.getNextVersion(),
                request.getNextAuditorType()
        );
        return ResponseEntity.ok(nextSubmission);
    }

    @GetMapping("/{id}/snapshots")
    public ResponseEntity<Map<String, Object>> getSnapshots(@PathVariable Long id) {
        String email = getCurrentUserEmail();
        User user = getCurrentUserDetails();
        Submission submission = submissionService.getSubmissionById(id)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + id));

        boolean isOwner = submission.getEmail().equalsIgnoreCase(email);
        boolean isIqac = "iqac".equalsIgnoreCase(user.getRole());
        boolean isVc = "vice-chancellor".equalsIgnoreCase(user.getRole());
        boolean isAuditor = user.getRole().toLowerCase().contains("auditor") || "auditor".equalsIgnoreCase(user.getAccountType());
        boolean isAdministrativeContributor = "administrative".equalsIgnoreCase(user.getRole())
                && "administrative".equalsIgnoreCase(submission.getAuditType());
        
        boolean isAssignedAuditor = isAuditor && (submissionService.isAuditorAssigned(user, submission) || submissionService.isAuditorFallbackMatch(user, submission));

        if (!isOwner && !isIqac && !isVc && !isAssignedAuditor && !isAdministrativeContributor) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(Map.of("data", submissionService.getVersionHistoryForSubmission(id)));
    }

    @Data
    public static class FormSubmissionRequest {
        private String auditType;
        private String valuesData;
        private String tablesData;
        private String attachments;
        private String status;
        private boolean sharedAdministrativeForm;
        private String action;
        private String contributorPost;
        private List<String> sections;
        private String forwardedAuditorType;
        private String forwardedAuditCategory;
        private List<Long> forwardedToAuditorIds;
        private List<String> forwardedToAuditorNames;
        private List<String> forwardedToAuditorEmails;
        private List<String> forwardedAdministrativePosts;
        private List<String> forwardedToAuditorPosts;
        private Boolean auditorCorrectionRequested;
        private Boolean correctionRequestedForAuditor;
        private Boolean requiresAuditorResubmission;
        private String auditorCorrectionMessage;
        private String auditorCorrectionRequestedBy;
        private String auditorCorrectionRequestedByRole;
        private String auditorCorrectionRequestedOn;
        private String auditorResubmittedAt;
        private String remarks;
    }

    @Data
    public static class ReviewRequest {
        private String status; // APPROVED/FINAL, UNDER_REVIEW
        private String remarks;
        private String reportCategory;
        private String auditCycle;
        private Integer version;
        private String valuesData;
        private String tablesData;
        private String attachments;
    }

    @Data
    public static class NextCycleRequest {
        private boolean preserveApprovedVersion = true;
        private Long previousApprovedSubmissionId;
        private Integer nextVersion;
        private String nextAuditorType;
    }

    public void downloadAttachments(@PathVariable Long id, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        downloadAttachments(id, false, response);
    }

    @GetMapping("/{id}/attachments/download")
    @PreAuthorize("hasAnyRole('ROLE_VICE-CHANCELLOR', 'ROLE_IQAC')")
    public void downloadAttachments(@PathVariable Long id,
                                    @RequestParam(required = false, defaultValue = "false") boolean includeAllContributors,
                                    jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        User user = getCurrentUserDetails();
        Submission submission = submissionService.getSubmissionById(id)
                .orElseThrow(() -> new com.director_appraisal.director_appraisal.exception.NotFoundException("Submission not found"));

        boolean isIqac = "iqac".equalsIgnoreCase(user.getRole());
        boolean isVc = "vice-chancellor".equalsIgnoreCase(user.getRole());
        if (!isIqac && !isVc) {
            throw new SecurityException("Only IQAC or VC may download attachments");
        }

        if (isVc) {
            boolean statusAllowed = List.of("AUDITOR_COMPLETED", "APPROVED", "FINAL").contains(submission.getStatus().toUpperCase());
            if (!statusAllowed) {
                throw new SecurityException("Unauthorized access to submission");
            }
        } else {
            // IQAC
            boolean statusAllowed = List.of("SUBMITTED", "UNDER_REVIEW", "AUDITOR_COMPLETED", "APPROVED", "FINAL")
                    .contains(submission.getStatus().toUpperCase());
            if (!statusAllowed) {
                throw new SecurityException("Unauthorized access to submission");
            }
        }

        // submission.attachments remains primary, but table/value payloads may contain section-specific attachments.
        List<ExtractedAttachment> attachments = new java.util.ArrayList<>();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        
        try {
            if (submission.getAttachments() != null && !submission.getAttachments().isBlank()) {
                collectAttachments(mapper.readTree(submission.getAttachments()), attachments);
            }
            if (submission.getTablesData() != null && !submission.getTablesData().isBlank()) {
                collectAttachments(mapper.readTree(submission.getTablesData()), attachments);
            }
            if (includeAllContributors && "administrative".equalsIgnoreCase(submission.getAuditType())
                    && submission.getValuesData() != null && !submission.getValuesData().isBlank()) {
                collectAttachments(mapper.readTree(submission.getValuesData()), attachments);
            }
        } catch (Exception e) {
            // Ignore parse errors, just use what we can parse
        }
        attachments = deduplicateAttachments(attachments);

        if (attachments.isEmpty()) {
            throw new com.director_appraisal.director_appraisal.exception.NotFoundException("No attachments found for this submission");
        }

        String zipFileName = getZipFileName(submission);

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipFileName + "\"");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        java.util.Set<String> usedPaths = new java.util.HashSet<>();
        List<String> missingFiles = new java.util.ArrayList<>();

        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(response.getOutputStream())) {
            for (ExtractedAttachment att : attachments) {
                if (att.url == null || att.url.isBlank()) {
                    continue;
                }

                String folderPath = getZipFolderPath(att, submission.getAuditType());
                String sanitizedName = sanitizeFilename(att.fileName);
                String zipEntryPath = folderPath + sanitizedName;

                if (usedPaths.contains(zipEntryPath)) {
                    int dotIndex = sanitizedName.lastIndexOf('.');
                    String namePart = dotIndex >= 0 ? sanitizedName.substring(0, dotIndex) : sanitizedName;
                    String extPart = dotIndex >= 0 ? sanitizedName.substring(dotIndex) : "";
                    int counter = 1;
                    String newEntryPath;
                    do {
                        newEntryPath = folderPath + namePart + "_" + counter + extPart;
                        counter++;
                    } while (usedPaths.contains(newEntryPath));
                    zipEntryPath = newEntryPath;
                }
                usedPaths.add(zipEntryPath);

                try (java.io.InputStream is = attachmentService.downloadAttachmentStream(att.url)) {
                    java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(zipEntryPath);
                    zos.putNextEntry(entry);
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        zos.write(buffer, 0, bytesRead);
                    }
                    zos.closeEntry();
                } catch (Exception e) {
                    System.err.println("Skipping inaccessible attachment: " + att.url + " - " + e.getMessage());
                    missingFiles.add("File: " + att.fileName + ", URL: " + att.url + ", Error: " + e.getMessage());
                }
            }

            if (!missingFiles.isEmpty()) {
                java.util.zip.ZipEntry missingEntry = new java.util.zip.ZipEntry("missing-files.txt");
                zos.putNextEntry(missingEntry);
                String content = String.join("\n", missingFiles);
                zos.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
    }

    private void collectAttachments(com.fasterxml.jackson.databind.JsonNode node, List<ExtractedAttachment> list) {
        collectAttachments(node, list, null);
    }

    private void collectAttachments(com.fasterxml.jackson.databind.JsonNode node, List<ExtractedAttachment> list, String sectionContext) {
        if (node == null) return;
        if (node.isObject()) {
            String currentSection = sectionContext;
            if (node.has("url") && node.get("url").isTextual()) {
                String url = node.get("url").asText();
                if (url.startsWith("/uploads/") || url.contains(".storage.googleapis.com") || url.contains("storage.googleapis.com")) {
                    ExtractedAttachment att = new ExtractedAttachment();
                    att.url = url;
                    att.objectKey = extractObjectKey(url);
                    att.sectionId = currentSection;
                    
                    if (node.has("fileName") && node.get("fileName").isTextual()) {
                        att.fileName = node.get("fileName").asText();
                    } else if (node.has("name") && node.get("name").isTextual()) {
                        att.fileName = node.get("name").asText();
                    } else {
                        int lastSlash = url.lastIndexOf('/');
                        att.fileName = lastSlash >= 0 ? url.substring(lastSlash + 1) : "attachment.pdf";
                    }
                    
                    if (node.has("sectionId") && node.get("sectionId").isTextual()) {
                        att.sectionId = node.get("sectionId").asText();
                    }
                    if (node.has("tableId") && node.get("tableId").isTextual()) {
                        att.tableId = node.get("tableId").asText();
                    }
                    if (node.has("rowIndex") && node.get("rowIndex").isNumber()) {
                        att.rowIndex = node.get("rowIndex").asInt();
                    }
                    if (node.has("column") && node.get("column").isTextual()) {
                        att.column = node.get("column").asText();
                    }
                    if (node.has("id")) {
                        att.id = node.get("id").asText();
                    } else if (node.has("attachmentId")) {
                        att.id = node.get("attachmentId").asText();
                    }
                    if (node.has("objectKey")) {
                        att.objectKey = node.get("objectKey").asText();
                    } else if (node.has("storageObjectKey")) {
                        att.objectKey = node.get("storageObjectKey").asText();
                    }
                    if (node.has("checksum")) {
                        att.checksum = node.get("checksum").asText();
                    } else if (node.has("sha256")) {
                        att.checksum = node.get("sha256").asText();
                    }
                    if (node.has("size")) {
                        att.size = node.get("size").asText();
                    } else if (node.has("fileSize")) {
                        att.size = node.get("fileSize").asText();
                    }
                    list.add(att);
                }
            }
            node.fields().forEachRemaining(entry ->
                    collectAttachments(entry.getValue(), list, resolveAttachmentSectionContext(entry.getKey(), currentSection)));
        } else if (node.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode item : node) {
                collectAttachments(item, list, sectionContext);
            }
        }
    }

    private String resolveAttachmentSectionContext(String key, String currentSection) {
        if (key == null || key.isBlank()) {
            return currentSection;
        }
        String normalized = key.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (normalized.contains("scholarshipsummary")
                || normalized.contains("scholarshipdetails")
                || normalized.contains("scholarshipstudents")
                || normalized.contains("scholarshipstudentdetails")
                || normalized.contains("coursesoffered")
                || normalized.contains("studentstatistics")
                || normalized.contains("statutory")
                || normalized.contains("auditrecords")) {
            return "registrar-part-a";
        }
        if (normalized.contains("infrastructure")
                || normalized.contains("library") || normalized.contains("eresource")
                || normalized.contains("researchresource")) {
            return "registrar-part-c";
        }
        if (normalized.contains("faculty") || normalized.contains("staff") || normalized.contains("bogmom")) {
            return "hr-part-b";
        }
        if (normalized.contains("hackathon")
                || normalized.contains("ideation")
                || normalized.contains("cultural")
                || normalized.contains("sportsactivities")
                || normalized.contains("sportsclubs")
                || normalized.contains("community")
                || normalized.contains("adminstudentawards")
                || normalized.contains("awardsprizesrecognitions")) {
            return "dean-student-welfare-part-d";
        }
        if (normalized.contains("parte") || normalized.contains("parteschools")
                || normalized.contains("placement") || normalized.contains("trainingactivities")
                || normalized.contains("industrycollaboration")) {
            return "dean-placement-part-e";
        }
        return currentSection;
    }

    private List<ExtractedAttachment> deduplicateAttachments(List<ExtractedAttachment> attachments) {
        java.util.Set<String> seenKeys = new java.util.HashSet<>();
        List<ExtractedAttachment> deduped = new java.util.ArrayList<>();
        List<ExtractedAttachment> noKey = new java.util.ArrayList<>();
        for (ExtractedAttachment attachment : attachments) {
            List<String> keys = attachmentIdentityKeys(attachment);
            if (keys.isEmpty()) {
                noKey.add(attachment);
                continue;
            }
            String matchedKey = keys.stream().filter(seenKeys::contains).findFirst().orElse(null);
            if (matchedKey != null) {
                System.err.println("Skipping duplicate attachment in ZIP: " + matchedKey);
                continue;
            }
            seenKeys.addAll(keys);
            deduped.add(attachment);
        }
        deduped.addAll(noKey);
        return deduped;
    }

    private List<String> attachmentIdentityKeys(ExtractedAttachment attachment) {
        List<String> keys = new java.util.ArrayList<>();
        if (notBlank(attachment.id)) {
            keys.add("id:" + attachment.id.trim());
        }
        if (notBlank(attachment.objectKey)) {
            keys.add("key:" + normalizeAttachmentUrl(attachment.objectKey));
        }
        if (notBlank(attachment.url)) {
            keys.add("url:" + normalizeAttachmentUrl(attachment.url));
        }
        if (notBlank(attachment.checksum)) {
            keys.add("checksum:" + attachment.checksum.trim().toLowerCase());
        }
        if (notBlank(attachment.fileName) && notBlank(attachment.size)) {
            keys.add("name-size:" + attachment.fileName.trim().toLowerCase() + ":" + attachment.size.trim());
        }
        return keys;
    }

    private String extractObjectKey(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        if (url.startsWith("/uploads/")) {
            return url.substring("/uploads/".length());
        }
        try {
            java.net.URI uri = java.net.URI.create(url);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            int bucketSeparator = path.indexOf('/');
            if ("storage.googleapis.com".equalsIgnoreCase(uri.getHost()) && bucketSeparator >= 0) {
                return path.substring(bucketSeparator + 1);
            }
            return path;
        } catch (Exception e) {
            return url;
        }
    }

    private String normalizeAttachmentUrl(String value) {
        String normalized = value == null ? "" : value.trim().replace("\\", "/");
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.toLowerCase();
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String getZipFileName(Submission submission) {
        String type = "academic".equalsIgnoreCase(submission.getAuditType()) ? "Academic" : "Administrative";
        String entityName = "Unknown";
        if ("academic".equalsIgnoreCase(submission.getAuditType())) {
            entityName = SchoolUtils.canonicalizeSchool(submission.getSchool());
        } else {
            entityName = userRepository.findByEmail(submission.getEmail())
                    .map(User::getPost)
                    .orElse(submission.getAdministrativePost());
            entityName = formatAdministrativePost(entityName);
        }
        if (entityName == null || entityName.isBlank()) {
            entityName = "Unknown";
        }
        entityName = entityName.replaceAll("[^A-Za-z0-9._-]", "_");
        String cycle = submission.getAuditCycle() != null ? submission.getAuditCycle() : submission.getAcademicYear();
        if (cycle == null || cycle.isBlank()) {
            cycle = "2025-2026";
        }
        cycle = cycle.replaceAll("[^A-Za-z0-9._-]", "_");
        return type + "_" + entityName + "_" + cycle + ".zip";
    }

    private String formatAdministrativePost(String post) {
        if (post == null || post.isBlank()) {
            return "Unknown";
        }
        return switch (post.trim().toLowerCase()) {
            case "registrar" -> "Registrar";
            case "hr" -> "HR";
            case "dean-student-welfare" -> "Dean_Student_Welfare";
            case "dean-placement" -> "Dean_Placement";
            default -> post;
        };
    }

    private String getZipFolderPath(ExtractedAttachment att, String auditType) {
        String sec = att.sectionId != null ? att.sectionId.trim().toLowerCase() : "";
        if ("academic".equalsIgnoreCase(auditType)) {
            if (sec.contains("part-a") || sec.contains("parta")) {
                return "Part-A/";
            } else if (sec.contains("part-b") || sec.contains("partb")) {
                return "Part-B/";
            } else if (sec.contains("part-c") || sec.contains("partc")) {
                return "Part-C/";
            } else if (sec.contains("part-d") || sec.contains("partd")) {
                return "Part-D/";
            }
            return "Other-Attachments/";
        } else {
            if (sec.contains("registrar-part-a")) {
                return "Registrar/Part-A/";
            } else if (sec.contains("registrar-part-c")) {
                return "Registrar/Part-C/";
            } else if (sec.contains("hr-part-b")) {
                return "HR/Part-B/";
            } else if (sec.contains("dean-student-welfare-part-d")) {
                return "Dean-Student-Welfare/Part-D/";
            } else if (sec.contains("dean-placement-part-e")) {
                return "Dean-Placement/Part-E/";
            }
            if (sec.contains("section-a") || sec.contains("sectiona") || sec.contains("part-a") || sec.contains("parta")) {
                return "Section-A/";
            } else if (sec.contains("section-b") || sec.contains("sectionb") || sec.contains("part-b") || sec.contains("partb")) {
                return "Section-B/";
            } else if (sec.contains("section-c") || sec.contains("sectionc") || sec.contains("part-c") || sec.contains("partc")) {
                return "Section-C/";
            }
            return "Other-Attachments/";
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file.pdf";
        }
        filename = filename.replace("\\", "/");
        int lastSlash = filename.lastIndexOf('/');
        String base = lastSlash >= 0 ? filename.substring(lastSlash + 1) : filename;
        base = base.replace("..", "_");
        String clean = base.replaceAll("[^A-Za-z0-9._-]", "_");
        return clean.isBlank() ? "file.pdf" : clean;
    }

    @Data
    public static class ExtractedAttachment {
        private String fileName;
        private String url;
        private String sectionId;
        private String tableId;
        private Integer rowIndex;
        private String column;
        private String id;
        private String objectKey;
        private String checksum;
        private String size;
    }

    @Data
    public static class AuditorSubmitRequest {
        private Long auditorId;
        private String auditorName;
        private String auditorEmail;
        private String auditorType;
        private String auditCategory;
        private List<String> postsSubmitted;
        private List<String> submittedPosts;
        private List<String> administrativePosts;
        private List<String> assignedPosts;
        private List<String> posts;
        private List<String> assignmentKeys;
        private String submittedAt;
        private String reviewStatus;
        private String valuesData;
        private String tablesData;
        private String attachments;
        private Boolean auditorCorrectionRequested;
        private Boolean correctionRequestedForAuditor;
        private Boolean requiresAuditorResubmission;
    }
}
