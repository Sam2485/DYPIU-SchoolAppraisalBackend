package com.director_appraisal.director_appraisal.service;

import com.director_appraisal.director_appraisal.exception.ConflictException;
import com.director_appraisal.director_appraisal.model.Snapshot;
import com.director_appraisal.director_appraisal.model.Submission;
import com.director_appraisal.director_appraisal.model.SubmissionAuditorAssignment;
import com.director_appraisal.director_appraisal.model.User;
import com.director_appraisal.director_appraisal.repository.SnapshotRepository;
import com.director_appraisal.director_appraisal.repository.SubmissionAuditorAssignmentRepository;
import com.director_appraisal.director_appraisal.repository.SubmissionRepository;
import com.director_appraisal.director_appraisal.repository.UserAdministrativePostRepository;
import com.director_appraisal.director_appraisal.repository.UserRepository;
import com.director_appraisal.director_appraisal.util.SchoolUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private static final String STATUS_APPROVED_LEGACY = "APPROVED";
    private static final String STATUS_FINAL = "FINAL";
    private static final List<String> LOCKED_STATUSES = List.of("UNDER_REVIEW", "AUDITOR_COMPLETED", STATUS_APPROVED_LEGACY, STATUS_FINAL);
    private static final List<String> REVIEWER_VISIBLE_STATUSES = List.of("SUBMITTED", "UNDER_REVIEW", STATUS_APPROVED_LEGACY, STATUS_FINAL);
    private static final List<String> IQAC_VISIBLE_STATUSES = List.of("SUBMITTED", "UNDER_REVIEW", "AUDITOR_COMPLETED", STATUS_APPROVED_LEGACY, STATUS_FINAL);
    private static final List<String> VC_VISIBLE_STATUSES = List.of("AUDITOR_COMPLETED", STATUS_APPROVED_LEGACY, STATUS_FINAL);
    private static final List<String> NORMALIZED_TABLE_STATUSES = List.of("SUBMITTED", "UNDER_REVIEW", "AUDITOR_COMPLETED", STATUS_APPROVED_LEGACY, STATUS_FINAL);
    private static final List<String> EDITABLE_CYCLE_STATUSES = List.of("DRAFT", "SUBMITTED", "SENT_BACK");
    private static final List<String> ADMIN_POSTS = List.of("registrar", "hr", "dean-student-welfare", "dean-placement");
    private static final String SHARED_ADMINISTRATIVE_EMAIL = "administrative.shared@dypiu.ac.in";

    private final SubmissionRepository submissionRepository;
    private final SnapshotRepository snapshotRepository;
    private final UserRepository userRepository;
    private final TableDataPromotionService tableDataPromotionService;
    private final SubmissionAuditorAssignmentRepository auditorAssignmentRepository;
    private final AcademicYearService academicYearService;
    private final UserAdministrativePostRepository userAdministrativePostRepository;
    private final AttachmentService attachmentService;

    @Transactional
    public Submission getOrCreateSharedAdministrativeDraft(User caller) {
        if (caller == null || !"administrative".equalsIgnoreCase(caller.getRole())) {
            throw new SecurityException("Only administrative authorities can access the shared administrative form");
        }
        String academicYear = academicYearService.getCurrentAcademicYearLabel();
        return getOrCreateSharedAdministrativeDraftForCycle(academicYear);
    }

    @Transactional
    public Submission getOrCreateSharedAdministrativeDraftForCycle(String cycleId) {
        if (cycleId == null || cycleId.isBlank()) {
            cycleId = academicYearService != null ? academicYearService.getCurrentAcademicYearLabel() : null;
            if (cycleId == null || cycleId.isBlank()) {
                cycleId = "2025-2026";
            }
        }
        String academicYear;
        String auditCycle;
        if (cycleId.length() == 9 && cycleId.contains("-")) { // e.g. 2025-2026
            academicYear = cycleId;
            String[] parts = cycleId.split("-");
            auditCycle = parts[0] + "-" + parts[1].substring(2);
        } else if (cycleId.length() == 7 && cycleId.contains("-")) { // e.g. 2025-26
            auditCycle = cycleId;
            String[] parts = cycleId.split("-");
            academicYear = parts[0] + "-20" + parts[1]; // e.g. 2025-2026
        } else {
            academicYear = cycleId;
            auditCycle = cycleId;
        }

        Optional<Submission> existing = submissionRepository.findFirstByEmailAndAuditTypeAndAcademicYearOrderByIdDesc(
                SHARED_ADMINISTRATIVE_EMAIL, "administrative", academicYear)
            .or(() -> submissionRepository.findFirstByEmailAndAuditTypeAndAuditCycleOrderByIdDesc(
                SHARED_ADMINISTRATIVE_EMAIL, "administrative", auditCycle));

        if (existing.isPresent()) {
            return existing.get();
        }

        Submission submission = Submission.builder()
                .email(SHARED_ADMINISTRATIVE_EMAIL)
                .auditType("administrative")
                .school("Administrative Office")
                .submittedBy("Administrative Authorities")
                .status("DRAFT")
                .valuesData(defaultSharedAdministrativeValuesData())
                .tablesData(defaultSharedAdministrativeTablesData())
                .attachments("[]")
                .academicYear(academicYear)
                .auditCycle(auditCycle)
                .reportCategory("INTERNAL")
                .version(1)
                .hasNextCycle(false)
                .build();
        Submission saved = submissionRepository.save(submission);
        saved.setRootSubmissionId(saved.getId());
        return submissionRepository.save(saved);
    }

    @Transactional
    public Submission submitAdministrativePart(String cycleId, User caller) {
        if (cycleId == null || cycleId.isBlank()) {
            cycleId = academicYearService != null ? academicYearService.getCurrentAcademicYearLabel() : null;
            if (cycleId == null || cycleId.isBlank()) {
                cycleId = "2025-2026";
            }
        }
        if (caller == null || !"administrative".equalsIgnoreCase(caller.getRole())) {
            throw new SecurityException("Only administrative authorities can submit sections");
        }
        String post = canonicalAdministrativePost(caller.getPost());
        if (post == null) {
            throw new SecurityException("Administrative post is required");
        }

        Submission submission = getOrCreateSharedAdministrativeDraftForCycle(cycleId);
        
        // Concurrency safety: row-level lock
        Submission lockedSubmission = submissionRepository.findByIdForUpdate(submission.getId())
                .orElseThrow(() -> new IllegalStateException("Submission went missing"));

        if ("SUBMITTED".equalsIgnoreCase(lockedSubmission.getStatus()) 
                || LOCKED_STATUSES.contains(lockedSubmission.getStatus().toUpperCase())) {
            return lockedSubmission;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            com.fasterxml.jackson.databind.node.ObjectNode valuesNode = objectNodeOrEmpty(mapper, lockedSubmission.getValuesData());
            com.fasterxml.jackson.databind.node.ObjectNode progress = administrativeProgressNode(mapper, valuesNode);

            progress.put(post, "SUBMITTED");
            valuesNode.set("administrativeProgress", progress);
            lockedSubmission.setValuesData(mapper.writeValueAsString(valuesNode));

            com.fasterxml.jackson.databind.node.ObjectNode detailsNode;
            if (lockedSubmission.getSubmittedByDetails() != null && !lockedSubmission.getSubmittedByDetails().isBlank()) {
                detailsNode = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(lockedSubmission.getSubmittedByDetails());
            } else {
                detailsNode = mapper.createObjectNode();
            }

            String jsonKey = toCamelCaseRole(post);
            com.fasterxml.jackson.databind.node.ObjectNode roleInfo = mapper.createObjectNode();
            roleInfo.put("submitted", true);
            roleInfo.put("submittedAt", LocalDateTime.now().toString());
            roleInfo.put("name", caller.getName());
            roleInfo.put("email", caller.getEmail());
            detailsNode.set(jsonKey, roleInfo);

            for (String p : ADMIN_POSTS) {
                String k = toCamelCaseRole(p);
                if (!detailsNode.has(k)) {
                    com.fasterxml.jackson.databind.node.ObjectNode emptyInfo = mapper.createObjectNode();
                    emptyInfo.put("submitted", false);
                    emptyInfo.putNull("submittedAt");
                    emptyInfo.putNull("name");
                    emptyInfo.putNull("email");
                    detailsNode.set(k, emptyInfo);
                }
            }

            lockedSubmission.setSubmittedByDetails(mapper.writeValueAsString(detailsNode));

            boolean allSubmitted = ADMIN_POSTS.stream()
                    .allMatch(requiredPost -> {
                        String st = progress.path(requiredPost).asText("DRAFT");
                        return "SUBMITTED".equalsIgnoreCase(st) || "APPROVED".equalsIgnoreCase(st);
                    });

            if (allSubmitted) {
                lockedSubmission.setStatus("SUBMITTED");
                lockedSubmission.setSubmittedAt(LocalDateTime.now());
            }

            Submission saved = submissionRepository.save(lockedSubmission);
            persistDataForStatus(saved);
            return saved;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to process submission", e);
        }
    }

    private String toCamelCaseRole(String post) {
        return switch (post) {
            case "registrar" -> "registrar";
            case "hr" -> "hr";
            case "dean-student-welfare" -> "deanStudentWelfare";
            case "dean-placement" -> "deanPlacement";
            default -> post;
        };
    }

    @Transactional
    public Submission saveSharedAdministrativeContribution(User caller, String contributorPost, List<String> sections,
                                                          String valuesData, String tablesData, String attachments,
                                                          boolean submitContribution) {
        Submission submission = getOrCreateSharedAdministrativeDraft(caller);
        Submission merged = mergeSharedAdministrativeContribution(submission.getId(), caller,
                null,
                contributorPost, sections, valuesData, tablesData, attachments);
        if (submitContribution) {
            return submitAdministrativePart(merged.getAcademicYear(), caller);
        }
        return merged;
    }

    @Transactional
    public Submission updateSharedAdministrativeContribution(Long id, User caller, String action, String contributorPost,
                                                            List<String> sections, String valuesData,
                                                            String tablesData, String attachments) {
        return mergeSharedAdministrativeContribution(id, caller, action, contributorPost, sections, valuesData, tablesData, attachments);
    }

    private Submission mergeSharedAdministrativeContribution(Long id, User caller, String action, String contributorPost,
                                                            List<String> sections, String valuesData,
                                                            String tablesData, String attachments) {
        Submission submission = submissionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + id));
        if (!SHARED_ADMINISTRATIVE_EMAIL.equalsIgnoreCase(submission.getEmail())
                || !"administrative".equalsIgnoreCase(submission.getAuditType())) {
            throw new IllegalArgumentException("Submission is not the shared administrative form");
        }
        String currentStatus = submission.getStatus();
        if (!"DRAFT".equalsIgnoreCase(currentStatus) && !"SENT_BACK".equalsIgnoreCase(currentStatus)) {
            throw new SecurityException("Shared administrative form is locked");
        }

        String callerPost = canonicalAdministrativePost(caller.getPost());
        String requestedPost = canonicalAdministrativePost(contributorPost);
        if (requestedPost == null) {
            requestedPost = callerPost;
        }
        if (callerPost == null || !callerPost.equals(requestedPost)) {
            throw new SecurityException("contributorPost does not match authenticated user");
        }

        java.util.Set<String> declaredSections = normalizeDeclaredSections(sections);
        java.util.Set<String> ownedSections = ownedAdministrativeSections(callerPost);
        if (declaredSections.isEmpty()) {
            declaredSections = ownedSections;
        }
        if (!ownedSections.containsAll(declaredSections)) {
            throw new SecurityException("User is not authorized for one or more declared sections");
        }

        boolean approving = "APPROVE_CONTRIBUTION".equalsIgnoreCase(action);
        ObjectMapper mapper = new ObjectMapper();
        try {
            com.fasterxml.jackson.databind.node.ObjectNode existingValues = objectNodeOrEmpty(mapper, submission.getValuesData());
            com.fasterxml.jackson.databind.node.ObjectNode progress = administrativeProgressNode(mapper, existingValues);
            String postProgress = progress.path(callerPost).asText();
            if (approving && ("APPROVED".equalsIgnoreCase(postProgress) || "SUBMITTED".equalsIgnoreCase(postProgress))) {
                throw new ConflictException("Contribution has already been approved");
            }
            if ("APPROVED".equalsIgnoreCase(postProgress) || "SUBMITTED".equalsIgnoreCase(postProgress)) {
                throw new SecurityException("Approved contribution is locked");
            }

            com.fasterxml.jackson.databind.node.ObjectNode mergedValues = mergeAdministrativeJson(
                    mapper,
                    submission.getValuesData(),
                    valuesData,
                    this::classifyAdministrativeValueSection,
                    declaredSections,
                    "valuesData"
            );
            com.fasterxml.jackson.databind.node.ObjectNode mergedTables = mergeAdministrativeJson(
                    mapper,
                    submission.getTablesData(),
                    tablesData,
                    this::classifyAdministrativeTableSection,
                    declaredSections,
                    "tablesData"
            );

            com.fasterxml.jackson.databind.node.ObjectNode mergedProgress = administrativeProgressNode(mapper, mergedValues);
            mergedProgress.put(callerPost, approving ? "APPROVED" : "IN_PROGRESS");
            mergedValues.set("administrativeProgress", mergedProgress);
            if (approving) {
                addAdministrativeApproval(mapper, mergedValues, callerPost, caller);
            }

            String preparedAttachments = mergeAttachmentMetadata(submission.getAttachments(), attachments);
            submission.setValuesData(mapper.writeValueAsString(mergedValues));
            submission.setTablesData(prepareTablesDataUpdate(submission, mapper.writeValueAsString(mergedTables), preparedAttachments));
            submission.setAttachments(preparedAttachments);

            boolean allApproved = ADMIN_POSTS.stream()
                    .allMatch(post -> {
                        String st = mergedProgress.path(post).asText("DRAFT");
                        return "APPROVED".equalsIgnoreCase(st) || "SUBMITTED".equalsIgnoreCase(st);
                    });
            if (allApproved) {
                submission.setStatus("SUBMITTED");
                submission.setSubmittedAt(LocalDateTime.now());
            }

            Submission saved = submissionRepository.save(submission);
            persistDataForStatus(saved);
            return saved;
        } catch (ConflictException | SecurityException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Shared administrative payload must be valid JSON", e);
        }
    }

    @Transactional
    public Submission getOrCreateDraft(String email, String auditType) {
        String academicYear = academicYearService.getCurrentAcademicYearLabel();
        Optional<Submission> editableCycle = submissionRepository
                .findFirstByEmailAndAuditTypeAndAcademicYearAndStatusInOrderByIdDesc(email, auditType, academicYear, EDITABLE_CYCLE_STATUSES);
        if (editableCycle.isPresent()) {
            return editableCycle.get();
        }

        Optional<Submission> latestCycle = submissionRepository.findFirstByEmailAndAuditTypeAndAcademicYearOrderByIdDesc(email, auditType, academicYear);
        if (latestCycle.isPresent()) {
            String statusUpper = latestCycle.get().getStatus().toUpperCase();
            if (isApprovalStatus(statusUpper)) {
                throw new SecurityException("Cannot edit an approved submission");
            }
            if (LOCKED_STATUSES.contains(statusUpper)) {
                throw new IllegalStateException("Cannot edit submission when status is " + statusUpper);
            }
        }

        Submission submission = Submission.builder()
                .email(email)
                .auditType(auditType)
                .status("DRAFT")
                .valuesData("{}")
                .tablesData("{}")
                .attachments("[]")
                .academicYear(academicYear)
                .auditCycle(toAuditCycle(academicYear))
                .reportCategory("INTERNAL")
                .version(1)
                .build();
        Submission saved = submissionRepository.save(submission);
        saved.setRootSubmissionId(saved.getId());
        return submissionRepository.save(saved);
    }

    @Transactional
    public Submission saveDraft(String email, String auditType, String school, String submittedBy, 
                                 String valuesData, String tablesData, String attachments) {
        Submission submission = getOrCreateDraft(email, auditType);
        
        // Updates can only occur before final approval or during draft/submitted/sent-back status
        String statusUpper = submission.getStatus().toUpperCase();
        if (LOCKED_STATUSES.contains(statusUpper)) {
            if (isApprovalStatus(statusUpper)) {
                throw new SecurityException("Cannot edit an approved submission");
            }
            throw new IllegalStateException("Cannot edit submission when status is " + statusUpper);
        }

        submission.setSchool(SchoolUtils.canonicalizeSchool(school));
        submission.setSubmittedBy(submittedBy);
        String preparedAttachments = deduplicateAttachmentMetadataJson(attachments);
        AdministrativePayload administrativePayload = prepareAdministrativePayload(submission, resolveUser(email), valuesData, tablesData, preparedAttachments, false);
        submission.setValuesData(administrativePayload.valuesData());
        submission.setTablesData(prepareTablesDataUpdate(submission, administrativePayload.tablesData(), preparedAttachments));
        submission.setAttachments(preparedAttachments);
        applySubmissionDefaults(submission);
        ensureVersion(submission);

        Submission saved = submissionRepository.save(submission);
        persistDataForStatus(saved);
        return saved;
    }

    @Transactional
    public Submission submitForm(String email, String auditType, String school, String submittedBy, 
                                 String valuesData, String tablesData, String attachments) {
        Submission submission = getOrCreateDraft(email, auditType);

        String statusUpper = submission.getStatus().toUpperCase();
        if (LOCKED_STATUSES.contains(statusUpper)) {
            if (isApprovalStatus(statusUpper)) {
                throw new SecurityException("Cannot edit an approved submission");
            }
            throw new IllegalStateException("Cannot edit submission when status is " + statusUpper);
        }

        submission.setSchool(SchoolUtils.canonicalizeSchool(school));
        submission.setSubmittedBy(submittedBy);
        String preparedAttachments = deduplicateAttachmentMetadataJson(attachments);
        AdministrativePayload administrativePayload = prepareAdministrativePayload(submission, resolveUser(email), valuesData, tablesData, preparedAttachments, true);
        submission.setValuesData(administrativePayload.valuesData());
        submission.setTablesData(prepareTablesDataUpdate(submission, administrativePayload.tablesData(), preparedAttachments));
        submission.setAttachments(preparedAttachments);
        if (administrativePayload.administrativePartial()) {
            if (administrativePayload.allAdministrativePostsSubmitted()) {
                submission.setStatus("SUBMITTED");
                submission.setSubmittedAt(LocalDateTime.now());
            } else {
                submission.setStatus("DRAFT");
            }
        } else {
            submission.setStatus("SUBMITTED");
            submission.setSubmittedAt(LocalDateTime.now());
        }
        applySubmissionDefaults(submission);
        ensureRootSubmissionId(submission);
        ensureVersion(submission);

        Submission saved = submissionRepository.save(submission);
        persistDataForStatus(saved);
        return saved;
    }

    @Transactional
    public Submission updateSubmissionById(Long id, String email, String school, String submittedBy, 
                                         String valuesData, String tablesData, String attachments) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + id));

        if (!submission.getEmail().equalsIgnoreCase(email)) {
            throw new IllegalStateException("You are not authorized to edit this submission");
        }

        String statusUpper = submission.getStatus().toUpperCase();
        if (LOCKED_STATUSES.contains(statusUpper)) {
            if (isApprovalStatus(statusUpper)) {
                throw new SecurityException("Cannot edit an approved submission");
            }
            throw new IllegalStateException("Cannot edit submission when status is " + statusUpper);
        }

        submission.setSchool(SchoolUtils.canonicalizeSchool(school));
        submission.setSubmittedBy(submittedBy);
        String preparedAttachments = deduplicateAttachmentMetadataJson(attachments);
        AdministrativePayload administrativePayload = prepareAdministrativePayload(submission, resolveUser(email), valuesData, tablesData, preparedAttachments, false);
        submission.setValuesData(administrativePayload.valuesData());
        submission.setTablesData(prepareTablesDataUpdate(submission, administrativePayload.tablesData(), preparedAttachments));
        submission.setAttachments(preparedAttachments);
        applySubmissionDefaults(submission);
        ensureVersion(submission);

        Submission saved = submissionRepository.save(submission);
        persistDataForStatus(saved);
        return saved;
    }

    public List<Submission> getSubmissionsForReviewer() {
        // VC & IQAC only view submitted/review/finalized forms.
        return submissionRepository.findByStatusIn(REVIEWER_VISIBLE_STATUSES);
    }

    public Optional<Submission> getSubmissionById(Long id) {
        return submissionRepository.findById(id);
    }

    @Transactional
    public Submission reviewSubmission(Long id, String status, String remarks, String reportCategory,
                                       String auditCycle, Integer version, String valuesData,
                                       String tablesData, String attachments, User reviewer) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + id));

        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Review status is required");
        }

        String requestedStatus = status.toUpperCase();
        if ("SENT_BACK".equals(requestedStatus)) {
            if (!"administrative".equalsIgnoreCase(submission.getAuditType())) {
                throw new IllegalArgumentException("Send back workflow is disabled.");
            }
            submission.setStatus("SENT_BACK");
            submission.setRemarks(remarks);
            ObjectMapper mapper = new ObjectMapper();
            try {
                com.fasterxml.jackson.databind.node.ObjectNode valuesNode = objectNodeOrEmpty(mapper, submission.getValuesData());
                com.fasterxml.jackson.databind.node.ObjectNode progress = mapper.createObjectNode();
                ADMIN_POSTS.forEach(post -> progress.put(post, "DRAFT"));
                valuesNode.set("administrativeProgress", progress);
                submission.setValuesData(mapper.writeValueAsString(valuesNode));
            } catch (Exception ignored) {}
            submission.setSubmittedByDetails(null);
            submission.setSubmittedAt(null);
            return submissionRepository.save(submission);
        }
        if (!List.of(STATUS_APPROVED_LEGACY, STATUS_FINAL, "UNDER_REVIEW", "SENT_BACK").contains(requestedStatus)) {
            throw new IllegalArgumentException("Invalid review status: " + status);
        }

        if (List.of(STATUS_APPROVED_LEGACY, STATUS_FINAL).contains(requestedStatus)) {
            if (!"AUDITOR_COMPLETED".equalsIgnoreCase(submission.getStatus())) {
                throw new IllegalStateException("Form can only be approved after the audit has been completed by an auditor");
            }
        }

        if (isApprovalStatus(requestedStatus)) {
            validateReviewer(reviewer);
            String expectedReportCategory = resolveExpectedReportCategoryForApproval(submission);
            validateReportCategory(reportCategory, expectedReportCategory);
            submission.setStatus(STATUS_APPROVED_LEGACY);
            submission.setReportCategory(expectedReportCategory);
            if (clean(auditCycle) != null) {
                submission.setAuditCycle(clean(auditCycle));
            }
            ensureAcademicYear(submission);
            if (version != null) {
                if (version < 1) {
                    throw new IllegalArgumentException("Version must be greater than zero");
                }
                if (submission.getVersion() != null && !submission.getVersion().equals(version)) {
                    throw new IllegalArgumentException("Approval version must match the submission version");
                }
                submission.setVersion(version);
            }
            ensureVersion(submission);
            ensureRootSubmissionId(submission);
            submission.setApprovedAt(LocalDateTime.now());
            submission.setApprovedByUserId(reviewer.getId());
            submission.setApprovedByName(reviewer.getName());
            submission.setApprovedByRole(reviewer.getRole());
            submission.setApprovedByDesignation(reviewer.getDesignation());
        } else {
            submission.setStatus(requestedStatus);
            ensureVersion(submission);
        }
        submission.setRemarks(remarks);
        submission.setReviewedBy(reviewer.getName());
        submission.setReviewedAt(LocalDateTime.now());
        String preparedAttachments = attachments != null ? deduplicateAttachmentMetadataJson(attachments) : submission.getAttachments();
        if (valuesData != null) submission.setValuesData(valuesData);
        if (tablesData != null) submission.setTablesData(prepareTablesDataUpdate(submission, tablesData, preparedAttachments));
        if (attachments != null) submission.setAttachments(preparedAttachments);
        applySubmissionDefaults(submission);

        Submission saved = submissionRepository.save(submission);
        persistDataForStatus(saved);
        return saved;
    }

    public List<Snapshot> getSnapshotsForSubmission(Long submissionId) {
        return snapshotRepository.findBySubmissionIdOrderByVersionDesc(submissionId);
    }

    public List<Map<String, Object>> getVersionHistoryForSubmission(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + submissionId));
        Long rootSubmissionId = resolveRootSubmissionId(submission);
        return submissionRepository.findLineage(rootSubmissionId).stream()
                .filter(item -> isApprovalStatus(item.getStatus()))
                .map(this::toVersionHistoryResponse)
                .toList();
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
                .academicYear(submission.getAcademicYear())
                .auditCycle(submission.getAuditCycle())
                .schoolGroup(submission.getSchoolGroup())
                .build();
        snapshotRepository.save(snapshot);
    }

    private void createDraftSnapshot(Submission submission) {
        String status = submission.getStatus();
        if (status == null) {
            return;
        }

        String statusUpper = status.toUpperCase();
        if (!"DRAFT".equals(statusUpper)) {
            return;
        }

        createSnapshot(submission);
    }

    private boolean isApprovalStatus(String status) {
        return status != null && (STATUS_APPROVED_LEGACY.equalsIgnoreCase(status) || STATUS_FINAL.equalsIgnoreCase(status));
    }

    public boolean isAuditorAssigned(User auditor, Submission submission) {
        List<SubmissionAuditorAssignment> assignments = auditorAssignmentRepository.findBySubmissionId(submission.getId());
        if (!assignments.isEmpty()) {
            return assignments.stream().anyMatch(assignment ->
                    assignment.getAuditorId().equals(auditor.getId())
                            || (assignment.getAuditorEmail() != null && assignment.getAuditorEmail().equalsIgnoreCase(auditor.getEmail())));
        }

        if (auditor.getId().equals(submission.getForwardedToAuditorId()) || 
            (submission.getForwardedToAuditorEmail() != null && 
             submission.getForwardedToAuditorEmail().equalsIgnoreCase(auditor.getEmail()))) {
            return true;
        }
        
        String idsStr = submission.getForwardedToAuditorIds();
        if (idsStr != null && !idsStr.isBlank()) {
            if (idsStr.contains(String.valueOf(auditor.getId()))) {
                return true;
            }
        }
        
        String emailsStr = submission.getForwardedToAuditorEmails();
        if (emailsStr != null && !emailsStr.isBlank()) {
            if (emailsStr.toLowerCase().contains("\"" + auditor.getEmail().toLowerCase() + "\"")) {
                return true;
            }
        }
        
        return false;
    }

    public boolean isAuditorFallbackMatch(User auditor, Submission submission) {
        if (submission.getId() != null && auditorAssignmentRepository.existsBySubmissionId(submission.getId())) {
            return false;
        }

        boolean statusMatch = List.of("UNDER_REVIEW", "AUDITOR_COMPLETED").contains(submission.getStatus().toUpperCase());
        if (!statusMatch) {
            return false;
        }

        String auditType = resolveAuditType(submission, null);
        if (auditType == null || auditType.isBlank()) {
            return false;
        }

        String auditorCategory = auditor.getCategory();
        if (auditorCategory == null || !auditorCategory.equalsIgnoreCase(auditType)) {
            return false;
        }

        String forwardedType = submission.getForwardedAuditorType();
        if (forwardedType == null || !forwardedType.equalsIgnoreCase(auditor.getAuditorType())) {
            return false;
        }

        if ("academic".equalsIgnoreCase(auditType)) {
            String subSchool = SchoolUtils.canonicalizeSchool(submission.getSchool());
            String audSchool = SchoolUtils.canonicalizeSchool(auditor.getSchool());
            return subSchool != null && subSchool.equalsIgnoreCase(audSchool);
        } else if ("administrative".equalsIgnoreCase(auditType)) {
            Optional<User> submitterOpt = userRepository.findByEmail(submission.getEmail());
            if (submitterOpt.isPresent()) {
                User submitter = submitterOpt.get();
                return auditorHasAdministrativePost(auditor, submitter.getPost());
            }
        }
        
        return false;
    }

    public void populateForwardingAuditors(Submission submission, String forwardedAuditorType) {
        if (forwardedAuditorType == null || forwardedAuditorType.isBlank()) {
            return;
        }
        
        String auditType = resolveAuditType(submission, null);
        if (auditType == null || auditType.isBlank()) {
            throw new IllegalArgumentException("Audit type is required to forward submission to auditor");
        }
        final String forwardingAuditType = auditType;

        List<User> allAuditors = userRepository.findByAccountType("auditor");
        
        List<User> matchedAuditors = allAuditors.stream()
                .filter(auditor -> {
                    if (auditor.getCategory() == null || !auditor.getCategory().equalsIgnoreCase(forwardingAuditType)) {
                        return false;
                    }
                    if (auditor.getAuditorType() == null || !auditor.getAuditorType().equalsIgnoreCase(forwardedAuditorType)) {
                        return false;
                    }
                    
                    if ("academic".equalsIgnoreCase(forwardingAuditType)) {
                        String subSchool = SchoolUtils.canonicalizeSchool(submission.getSchool());
                        String audSchool = SchoolUtils.canonicalizeSchool(auditor.getSchool());
                        return audSchool != null && audSchool.equalsIgnoreCase(subSchool);
                    } else {
                        Optional<User> submitterOpt = userRepository.findByEmail(submission.getEmail());
                        if (submitterOpt.isPresent()) {
                            User submitter = submitterOpt.get();
                            return auditorHasAdministrativePost(auditor, submitter.getPost());
                        }
                    }
                    return false;
                })
                .toList();
                
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<Long> ids = matchedAuditors.stream().map(User::getId).toList();
            List<String> names = matchedAuditors.stream().map(User::getName).toList();
            List<String> emails = matchedAuditors.stream().map(User::getEmail).toList();
            
            submission.setForwardedToAuditorIds(mapper.writeValueAsString(ids));
            submission.setForwardedToAuditorNames(mapper.writeValueAsString(names));
            submission.setForwardedToAuditorEmails(mapper.writeValueAsString(emails));
            
            if (!matchedAuditors.isEmpty()) {
                User first = matchedAuditors.get(0);
                submission.setForwardedToAuditorId(first.getId());
                submission.setForwardedToAuditorName(first.getName());
                submission.setForwardedToAuditorEmail(first.getEmail());
            } else {
                submission.setForwardedToAuditorId(null);
                submission.setForwardedToAuditorName(null);
                submission.setForwardedToAuditorEmail(null);
            }
        } catch (Exception e) {
            System.err.println("Error serializing auditors: " + e.getMessage());
        }
        
        submission.setForwardedAuditorType(forwardedAuditorType);
        submission.setForwardedAuditCategory(forwardingAuditType);
        submission.setForwardedAt(LocalDateTime.now());
    }

    private String resolveAuditType(Submission submission, String preferredAuditType) {
        if (preferredAuditType != null && !preferredAuditType.isBlank() && !"null".equalsIgnoreCase(preferredAuditType.trim())) {
            return preferredAuditType.trim().toLowerCase();
        }
        if (submission.getAuditType() != null && !submission.getAuditType().isBlank()) {
            return submission.getAuditType().trim().toLowerCase();
        }
        if (submission.getForwardedAuditCategory() != null && !submission.getForwardedAuditCategory().isBlank()) {
            return submission.getForwardedAuditCategory().trim().toLowerCase();
        }

        Optional<User> submitterOpt = userRepository.findByEmail(submission.getEmail());
        if (submitterOpt.isPresent()) {
            String submitterRole = submitterOpt.get().getRole();
            if ("director".equalsIgnoreCase(submitterRole)) {
                return "academic";
            }
            if ("administrative".equalsIgnoreCase(submitterRole)) {
                return "administrative";
            }
        }

        String school = submission.getSchool();
        if (school != null && !school.isBlank()) {
            if ("Administrative Office".equalsIgnoreCase(school.trim())) {
                return "administrative";
            }
            return "academic";
        }

        return null;
    }

    private String injectAuditorSignOff(String valuesData, User auditor) {
        if (valuesData == null || valuesData.isBlank()) {
            valuesData = "{}";
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            java.util.Map<String, Object> map = mapper.readValue(valuesData, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
            
            java.util.Map<String, Object> signOff = (java.util.Map<String, Object>) map.get("__auditSignOff");
            if (signOff == null) {
                signOff = new java.util.LinkedHashMap<>();
            }
            
            java.util.Map<String, Object> auditedBy = new java.util.LinkedHashMap<>();
            auditedBy.put("name", auditor.getName());
            auditedBy.put("designation", auditor.getDesignation());
            auditedBy.put("role", auditor.getRole());
            auditedBy.put("date", LocalDateTime.now().toString());
            
            signOff.put("auditedBy", auditedBy);
            map.put("__auditSignOff", signOff);
            
            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            System.err.println("Error injecting auditor sign off: " + e.getMessage());
            return valuesData;
        }
    }

    public List<Submission> getAllSubmissionsForUser(User user) {
        String role = user.getRole().toLowerCase();

        if ("iqac".equals(role)) {
            return submissionRepository.findByStatusIn(IQAC_VISIBLE_STATUSES);
        } else if ("vice-chancellor".equals(role)) {
            return submissionRepository.findByStatusIn(VC_VISIBLE_STATUSES);
        } else if (role.contains("auditor") || "auditor".equalsIgnoreCase(user.getAccountType())) {
            List<Submission> allSubmissions = submissionRepository.findAll();
            return allSubmissions.stream()
                    .filter(sub -> {
                        boolean matchesStatus = List.of("UNDER_REVIEW", "AUDITOR_COMPLETED").contains(sub.getStatus().toUpperCase());
                        if (!matchesStatus) {
                            return false;
                        }
                        return isAuditorAssigned(user, sub) || isAuditorFallbackMatch(user, sub);
                    })
                    .toList();
        } else {
            return List.of();
        }
    }

    public List<Submission> getPreviousReports(User user, String academicYear) {
        validateReviewer(user);
        return submissionRepository.findByStatusIn(List.of(STATUS_APPROVED_LEGACY, STATUS_FINAL)).stream()
                .filter(submission -> academicYear == null || academicYear.isBlank()
                        || academicYear.equals(submission.getAcademicYear())
                        || academicYear.equals(submission.getAuditCycle()))
                .toList();
    }

    @Transactional
    public Submission updateSubmission(Long id, User caller, String status, String forwardedAuditorType,
                                       String forwardedAuditCategory,
                                       List<Long> requestForwardedToAuditorIds,
                                       List<String> requestForwardedToAuditorNames,
                                       List<String> requestForwardedToAuditorEmails,
                                       String valuesData, String tablesData, String attachments) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + id));

        String callerRole = caller.getRole().toLowerCase();
        String callerEmail = caller.getEmail();

        boolean isOwner = submission.getEmail().equalsIgnoreCase(callerEmail);
        boolean isIqac = "iqac".equals(callerRole);
        boolean isAuditor = callerRole.contains("auditor") || "auditor".equalsIgnoreCase(caller.getAccountType());
        boolean isAssignedAuditor = isAuditor && (isAuditorAssigned(caller, submission) || isAuditorFallbackMatch(caller, submission));
        boolean isAdministrativeContributor = "administrative".equals(callerRole)
                && "administrative".equalsIgnoreCase(submission.getAuditType());

        if (!isOwner && !isIqac && !isAssignedAuditor && !isAdministrativeContributor) {
            throw new IllegalStateException("You are not authorized to edit this submission");
        }

        String currentStatus = submission.getStatus().toUpperCase();
        if (isApprovalStatus(currentStatus)) {
            throw new SecurityException("Cannot edit an approved submission");
        }

        if (submission.getAuditType() == null || submission.getAuditType().isBlank()) {
            String resolvedAuditType = resolveAuditType(submission, forwardedAuditCategory);
            if (resolvedAuditType != null && !resolvedAuditType.isBlank()) {
                submission.setAuditType(resolvedAuditType);
            }
        }

        if (isOwner && List.of("UNDER_REVIEW", "AUDITOR_COMPLETED").contains(currentStatus)) {
            throw new IllegalStateException("Submitters cannot edit forms once status transitions to UNDER_REVIEW or AUDITOR_COMPLETED");
        }

        if (status != null && !status.isBlank()) {
            String upperStatus = status.toUpperCase();
            if ("SENT_BACK".equals(upperStatus)) {
                if (!"administrative".equalsIgnoreCase(submission.getAuditType())) {
                    throw new IllegalArgumentException("Send back workflow is disabled.");
                }
                submission.setStatus("SENT_BACK");
                ObjectMapper mapper = new ObjectMapper();
                try {
                    com.fasterxml.jackson.databind.node.ObjectNode valuesNode = objectNodeOrEmpty(mapper, submission.getValuesData());
                    com.fasterxml.jackson.databind.node.ObjectNode progress = mapper.createObjectNode();
                    ADMIN_POSTS.forEach(post -> progress.put(post, "DRAFT"));
                    valuesNode.set("administrativeProgress", progress);
                    submission.setValuesData(mapper.writeValueAsString(valuesNode));
                } catch (Exception ignored) {}
                submission.setSubmittedByDetails(null);
                submission.setSubmittedAt(null);
            }
            if (upperStatus.equals("AUDITOR_COMPLETED")) {
                if (!isAssignedAuditor && !isIqac) {
                    throw new IllegalStateException("Only assigned auditors can complete the audit");
                }
                submission.setStatus("AUDITOR_COMPLETED");
                submission.setAuditorReviewedBy(caller.getName());
                submission.setAuditorReviewedByDesignation(caller.getDesignation());
                submission.setAuditorReviewedByRole(caller.getRole());
                submission.setAuditorReviewedOn(LocalDateTime.now());
                valuesData = injectAuditorSignOff(valuesData, caller);
            } else if (upperStatus.equals("UNDER_REVIEW")) {
                if (!isIqac) {
                    throw new IllegalStateException("Only IQAC can forward submissions for review");
                }
                assignSelectedAuditorsForReview(
                        submission,
                        forwardedAuditorType,
                        requestForwardedToAuditorIds
                );
                submission.setStatus("UNDER_REVIEW");
            } else if (upperStatus.equals("SUBMITTED")) {
                submission.setStatus("SUBMITTED");
            } else if (upperStatus.equals("DRAFT")) {
                submission.setStatus("DRAFT");
            } else if (isApprovalStatus(upperStatus)) {
                if (!isIqac) {
                    throw new IllegalStateException("Only IQAC can mark submissions as final");
                }
                if (!"AUDITOR_COMPLETED".equals(currentStatus)) {
                    throw new IllegalStateException("Form can only be finalized after the audit has been completed by an auditor");
                }
                submission.setStatus(STATUS_APPROVED_LEGACY);
                String expectedReportCategory = resolveExpectedReportCategoryForApproval(submission);
                submission.setReportCategory(expectedReportCategory);
                ensureAcademicYear(submission);
                submission.setApprovedAt(LocalDateTime.now());
                submission.setApprovedByUserId(caller.getId());
                submission.setApprovedByName(caller.getName());
                submission.setApprovedByRole(caller.getRole());
                submission.setApprovedByDesignation(caller.getDesignation());
            }
        }

        if (isIqac && (status == null || !"UNDER_REVIEW".equalsIgnoreCase(status))) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                if (requestForwardedToAuditorIds != null) {
                    submission.setForwardedToAuditorIds(mapper.writeValueAsString(requestForwardedToAuditorIds));
                }
                if (requestForwardedToAuditorNames != null) {
                    submission.setForwardedToAuditorNames(mapper.writeValueAsString(requestForwardedToAuditorNames));
                }
                if (requestForwardedToAuditorEmails != null) {
                    submission.setForwardedToAuditorEmails(mapper.writeValueAsString(requestForwardedToAuditorEmails));
                }
            } catch (Exception e) {
                System.err.println("Error serializing requested auditors: " + e.getMessage());
            }
        }

        String preparedAttachments = attachments != null ? deduplicateAttachmentMetadataJson(attachments) : submission.getAttachments();
        AdministrativePayload administrativePayload = prepareAdministrativePayload(submission, caller, valuesData, tablesData, preparedAttachments, "SUBMITTED".equalsIgnoreCase(status));
        if (valuesData != null || (administrativePayload.administrativePartial() && "SUBMITTED".equalsIgnoreCase(status))) {
            submission.setValuesData(administrativePayload.valuesData());
        }
        if (tablesData != null) submission.setTablesData(prepareTablesDataUpdate(submission, administrativePayload.tablesData(), preparedAttachments));
        if (attachments != null) submission.setAttachments(preparedAttachments);
        if (administrativePayload.administrativePartial() && "SUBMITTED".equalsIgnoreCase(status)) {
            if (administrativePayload.allAdministrativePostsSubmitted()) {
                submission.setStatus("SUBMITTED");
                submission.setSubmittedAt(LocalDateTime.now());
            } else {
                submission.setStatus("DRAFT");
            }
        }

        applySubmissionDefaults(submission);
        ensureVersion(submission);

        Submission saved = submissionRepository.save(submission);
        persistDataForStatus(saved);
        return saved;
    }

    @Transactional
    public Submission createNextCycle(Long approvedSubmissionId, User caller, boolean preserveApprovedVersion,
                                      Long previousApprovedSubmissionId, Integer nextVersion,
                                      String nextAuditorType) {
        validateReviewer(caller);
        Submission approved = submissionRepository.findByIdForUpdate(approvedSubmissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + approvedSubmissionId));

        if (!"APPROVED".equalsIgnoreCase(approved.getStatus())) {
            throw new IllegalStateException("Next audit cycle can only be created from an approved submission");
        }

        if (!"INTERNAL".equalsIgnoreCase(approved.getReportCategory())) {
            throw new IllegalArgumentException("Source reportCategory must be INTERNAL");
        }

        if (approved.getVersion() == null || approved.getVersion() != 1) {
            throw new IllegalArgumentException("Source version must be exactly 1");
        }

        if (!"EXTERNAL".equalsIgnoreCase(clean(nextAuditorType))) {
            throw new IllegalArgumentException("Next auditor type must be EXTERNAL");
        }

        if (Boolean.TRUE.equals(approved.getHasNextCycle()) || approved.getNextVersionId() != null) {
            throw new ConflictException("Next cycle already exists");
        }

        Long rootSubmissionId = resolveRootSubmissionId(approved);
        int expectedNextVersion = 2;

        // Check if next cycle already exists in the database (e.g. from prior deployment or concurrency)
        Optional<Submission> existingByRootAndVersion = submissionRepository.findByRootSubmissionIdAndVersion(rootSubmissionId, expectedNextVersion);
        if (existingByRootAndVersion.isPresent()) {
            throw new ConflictException("Next cycle already exists");
        }

        Optional<Submission> existingByParent = submissionRepository.findByParentSubmissionId(approved.getId());
        if (existingByParent.isPresent()) {
            throw new ConflictException("Next cycle already exists");
        }
        
        int requestedNextVersion = nextVersion != null ? nextVersion : expectedNextVersion;
        if (requestedNextVersion != expectedNextVersion) {
            throw new IllegalArgumentException("Next version must be " + expectedNextVersion);
        }
        if (previousApprovedSubmissionId != null && !previousApprovedSubmissionId.equals(approved.getId())) {
            throw new IllegalArgumentException("Previous approved submission ID must match the approved source submission");
        }

        Submission next = Submission.builder()
                .email(approved.getEmail())
                .auditType(approved.getAuditType())
                .school(approved.getSchool())
                .submittedBy(approved.getSubmittedBy())
                .status("DRAFT")
                .valuesData(clearCurrentCycleReviewData(approved.getValuesData(), approved.getAuditType()))
                .tablesData(clearCurrentCycleReviewData(approved.getTablesData(), approved.getAuditType()))
                .attachments(preserveApprovedVersion ? approved.getAttachments() : "[]")
                .version(requestedNextVersion)
                .academicYear(approved.getAcademicYear() != null ? approved.getAcademicYear() : approved.getAuditCycle())
                .auditCycle(approved.getAuditCycle())
                .reportCategory("EXTERNAL")
                .schoolGroup(approved.getSchoolGroup())
                .administrativePost(approved.getAdministrativePost())
                .rootSubmissionId(rootSubmissionId)
                .parentSubmissionId(approved.getId())
                .previousApprovedSubmissionId(previousApprovedSubmissionId != null ? previousApprovedSubmissionId : approved.getId())
                .createdFromVersion(approved.getVersion())
                .forwardedAuditorType("external")
                .forwardedAuditCategory(approved.getAuditType())
                .hasNextCycle(false)
                .nextVersionId(null)
                .build();

        Submission saved = submissionRepository.save(next);
        
        approved.setHasNextCycle(true);
        approved.setNextVersionId(saved.getId());
        submissionRepository.save(approved);

        persistDataForStatus(saved);
        return saved;
    }

    private void persistDataForStatus(Submission submission) {
        if (usesNormalizedTables(submission.getStatus())) {
            tableDataPromotionService.syncNormalizedTablesAndClearSnapshots(submission);
            return;
        }

        createDraftSnapshot(submission);
    }

    private boolean usesNormalizedTables(String status) {
        return status != null && NORMALIZED_TABLE_STATUSES.contains(status.toUpperCase());
    }

    private void assignSelectedAuditorsForReview(Submission submission, String forwardedAuditorType, List<Long> selectedAuditorIds) {
        if (submission.getId() == null) {
            throw new IllegalStateException("Submission must be saved before auditor assignment");
        }
        if (submission.getForwardedAt() != null || auditorAssignmentRepository.existsBySubmissionId(submission.getId())) {
            throw new ConflictException("Submission has already been forwarded to auditor");
        }
        if (selectedAuditorIds == null || selectedAuditorIds.isEmpty()) {
            throw new IllegalArgumentException("At least one auditor must be selected");
        }

        String requestedType = cleanAuditorType(forwardedAuditorType);
        if (requestedType == null || !List.of("internal", "external").contains(requestedType)) {
            throw new IllegalArgumentException("Forwarded auditor type must be INTERNAL or EXTERNAL");
        }

        String auditType = resolveAuditType(submission, submission.getForwardedAuditCategory());
        if (auditType == null || auditType.isBlank()) {
            throw new IllegalArgumentException("Audit type is required to forward submission to auditor");
        }

        List<User> selectedAuditors = selectedAuditorIds.stream()
                .distinct()
                .map(id -> userRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Selected auditor not found: " + id)))
                .toList();

        String submitterPost = resolveAdministrativePost(submission);
        String submissionSchool = SchoolUtils.canonicalizeSchool(submission.getSchool());
        for (User auditor : selectedAuditors) {
            validateSelectedAuditor(auditor, auditType, requestedType, submissionSchool, submitterPost);
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            submission.setForwardedToAuditorIds(mapper.writeValueAsString(selectedAuditors.stream().map(User::getId).toList()));
            submission.setForwardedToAuditorNames(mapper.writeValueAsString(selectedAuditors.stream().map(User::getName).toList()));
            submission.setForwardedToAuditorEmails(mapper.writeValueAsString(selectedAuditors.stream().map(User::getEmail).toList()));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to store selected auditor metadata", e);
        }

        User first = selectedAuditors.get(0);
        submission.setForwardedToAuditorId(first.getId());
        submission.setForwardedToAuditorName(first.getName());
        submission.setForwardedToAuditorEmail(first.getEmail());
        submission.setForwardedAuditorType(requestedType);
        submission.setForwardedAuditCategory(auditType);
        submission.setForwardedAt(LocalDateTime.now());

        LocalDateTime assignedAt = LocalDateTime.now();
        selectedAuditors.forEach(auditor -> auditorAssignmentRepository.save(SubmissionAuditorAssignment.builder()
                .submissionId(submission.getId())
                .auditorId(auditor.getId())
                .auditorName(auditor.getName())
                .auditorEmail(auditor.getEmail())
                .auditorType(requestedType)
                .category(auditType)
                .assignedAt(assignedAt)
                .build()));
    }

    private void validateSelectedAuditor(User auditor, String auditType, String requestedType, String submissionSchool, String submitterPost) {
        String role = normalize(auditor.getRole());
        String accountType = normalize(auditor.getAccountType());
        if (!"auditor".equals(accountType) && (role == null || !role.contains("auditor"))) {
            throw new IllegalArgumentException("Selected user is not an auditor: " + auditor.getEmail());
        }
        if (auditor.getCategory() == null || !auditor.getCategory().equalsIgnoreCase(auditType)) {
            throw new IllegalArgumentException("Selected auditor category does not match submission audit type: " + auditor.getEmail());
        }
        if (auditor.getAuditorType() == null || !auditor.getAuditorType().equalsIgnoreCase(requestedType)) {
            throw new IllegalArgumentException("Selected auditor type does not match requested auditor type: " + auditor.getEmail());
        }
        if ("academic".equalsIgnoreCase(auditType)) {
            String auditorSchool = SchoolUtils.canonicalizeSchool(auditor.getSchool());
            if (submissionSchool == null || auditorSchool == null || !submissionSchool.equalsIgnoreCase(auditorSchool)) {
                throw new IllegalArgumentException("Academic auditor must match the submission school: " + auditor.getEmail());
            }
        } else if ("administrative".equalsIgnoreCase(auditType)) {
            if (submitterPost == null || !auditorHasAdministrativePost(auditor, submitterPost)) {
                throw new IllegalArgumentException("Administrative auditor must match the administrative post: " + auditor.getEmail());
            }
        }
    }

    private boolean auditorHasAdministrativePost(User auditor, String post) {
        if (post == null || post.isBlank()) {
            return false;
        }
        String normalizedPost = post.trim().toLowerCase();
        if (auditor.getId() != null && userAdministrativePostRepository.existsByUserIdAndPost(auditor.getId(), normalizedPost)) {
            return true;
        }
        return auditor.getPost() != null && auditor.getPost().equalsIgnoreCase(normalizedPost);
    }

    private String resolveAdministrativePost(Submission submission) {
        if (submission.getAdministrativePost() != null && !submission.getAdministrativePost().isBlank()) {
            return submission.getAdministrativePost().trim().toLowerCase();
        }
        return userRepository.findByEmail(submission.getEmail())
                .map(User::getPost)
                .map(this::normalize)
                .orElse(null);
    }

    private String resolveExpectedReportCategoryForApproval(Submission submission) {
        String fromAuditorType = cleanAuditorType(submission.getForwardedAuditorType());
        String expectedFromAuditor = null;
        if ("internal".equals(fromAuditorType)) {
            expectedFromAuditor = "INTERNAL";
        } else if ("external".equals(fromAuditorType)) {
            expectedFromAuditor = "EXTERNAL";
        }

        String expectedFromVersion = null;
        if (submission.getVersion() != null) {
            if (submission.getVersion() == 1) {
                expectedFromVersion = "INTERNAL";
            } else if (submission.getVersion() == 2) {
                expectedFromVersion = "EXTERNAL";
            }
        }

        if (expectedFromAuditor != null && expectedFromVersion != null && !expectedFromAuditor.equals(expectedFromVersion)) {
            throw new IllegalStateException("Report category does not match assigned auditor type and version");
        }
        if (expectedFromAuditor != null) {
            return expectedFromAuditor;
        }
        if (expectedFromVersion != null) {
            return expectedFromVersion;
        }
        return "INTERNAL";
    }

    private void validateReportCategory(String reportCategory, String expectedReportCategory) {
        if (reportCategory == null || reportCategory.isBlank()) {
            throw new IllegalArgumentException("Report category is required for approval");
        }
        String normalized = reportCategory.trim().toUpperCase();
        if (!List.of("INTERNAL", "EXTERNAL").contains(normalized)) {
            throw new IllegalArgumentException("Report category must be INTERNAL or EXTERNAL");
        }
        if (!normalized.equals(expectedReportCategory)) {
            throw new IllegalArgumentException("Report category must be " + expectedReportCategory + " for this submission");
        }
    }

    private void validateReviewer(User caller) {
        String role = caller.getRole() == null ? "" : caller.getRole().toLowerCase();
        if (!List.of("iqac", "vice-chancellor").contains(role)) {
            throw new SecurityException("Only IQAC or VC can perform this action");
        }
    }

    private void ensureVersion(Submission submission) {
        if (submission.getVersion() == null || submission.getVersion() < 1) {
            submission.setVersion(1);
        }
    }

    private void ensureRootSubmissionId(Submission submission) {
        if (submission.getRootSubmissionId() == null && submission.getId() != null) {
            submission.setRootSubmissionId(submission.getId());
        }
    }

    private Long resolveRootSubmissionId(Submission submission) {
        return submission.getRootSubmissionId() != null ? submission.getRootSubmissionId() : submission.getId();
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String cleanAuditorType(String nextAuditorType) {
        return nextAuditorType == null || nextAuditorType.isBlank() ? null : nextAuditorType.trim().toLowerCase();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private void ensureAcademicYear(Submission submission) {
        if (submission.getAcademicYear() == null || submission.getAcademicYear().isBlank()) {
            String year = submission.getAuditCycle();
            if (year == null || year.isBlank()) {
                year = academicYearService.getCurrentAcademicYearLabel();
            }
            submission.setAcademicYear(toAcademicYear(year));
        }
        if (submission.getAuditCycle() == null || submission.getAuditCycle().isBlank()) {
            submission.setAuditCycle(toAuditCycle(submission.getAcademicYear()));
        }
        if (submission.getReportCategory() == null || submission.getReportCategory().isBlank()) {
            submission.setReportCategory(submission.getVersion() != null && submission.getVersion() == 2 ? "EXTERNAL" : "INTERNAL");
        }
    }

    private void applySubmissionDefaults(Submission submission) {
        ensureAcademicYear(submission);
        if ("academic".equalsIgnoreCase(submission.getAuditType())) {
            String canonicalSchool = SchoolUtils.canonicalizeSchool(submission.getSchool());
            submission.setSchool(canonicalSchool);
            String group = SchoolUtils.schoolGroup(canonicalSchool);
            if ("all".equalsIgnoreCase(group)) {
                group = null;
            }
            submission.setSchoolGroup(group);
        }
    }

    private String defaultSharedAdministrativeValuesData() {
        ObjectMapper mapper = new ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode root = mapper.createObjectNode();
        com.fasterxml.jackson.databind.node.ObjectNode progress = mapper.createObjectNode();
        ADMIN_POSTS.forEach(post -> progress.put(post, "DRAFT"));
        root.set("administrativeProgress", progress);
        root.set("administrativeApprovals", mapper.createObjectNode());
        try {
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"administrativeProgress\":{\"registrar\":\"DRAFT\",\"hr\":\"DRAFT\",\"dean-student-welfare\":\"DRAFT\",\"dean-placement\":\"DRAFT\"},\"administrativeApprovals\":{}}";
        }
    }

    private String defaultSharedAdministrativeTablesData() {
        ObjectMapper mapper = new ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode root = mapper.createObjectNode();
        administrativeDefaultTableKeys().forEach(key -> {
            com.fasterxml.jackson.databind.node.ArrayNode rows = mapper.createArrayNode();
            com.fasterxml.jackson.databind.node.ObjectNode row = mapper.createObjectNode();
            row.put("Sr No", "1");
            rows.add(row);
            root.set(key, rows);
        });
        try {
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            return "{}";
        }
    }

    private List<String> administrativeDefaultTableKeys() {
        return List.of(
                "studentStatistics",
                "coursesOffered",
                "scholarshipSummary",
                "scholarshipStudents",
                "facultyInformation",
                "facultyTenure",
                "facultyExperience",
                "supportingStaff",
                "staffTraining",
                "buildingInfrastructure",
                "libraryInfrastructure",
                "eResources",
                "itInfrastructure",
                "sportsFacilities",
                "divyangajanFacilities",
                "researchResources",
                "statutoryBodies",
                "auditRecords",
                "hackathons",
                "culturalActivities",
                "sportsActivities",
                "communityActivities",
                "adminStudentAwards",
                "trainingActivities",
                "industryCollaborations"
        );
    }

    private java.util.Set<String> normalizeDeclaredSections(List<String> sections) {
        if (sections == null || sections.isEmpty()) {
            return java.util.Set.of();
        }
        java.util.Set<String> normalized = new java.util.HashSet<>();
        for (String section : sections) {
            if (section == null || section.isBlank()) {
                continue;
            }
            String clean = section.trim().toUpperCase().replace("PART-", "").replace("PART_", "").replace("PART ", "");
            if (!List.of("A", "B", "C", "D", "E").contains(clean)) {
                throw new IllegalArgumentException("Invalid administrative section: " + section);
            }
            normalized.add(clean);
        }
        return normalized;
    }

    private void addAdministrativeApproval(ObjectMapper mapper, com.fasterxml.jackson.databind.node.ObjectNode values,
                                           String post, User approver) {
        com.fasterxml.jackson.databind.JsonNode existing = values.get("administrativeApprovals");
        com.fasterxml.jackson.databind.node.ObjectNode approvals = existing != null && existing.isObject()
                ? (com.fasterxml.jackson.databind.node.ObjectNode) existing.deepCopy()
                : mapper.createObjectNode();
        com.fasterxml.jackson.databind.node.ObjectNode approval = mapper.createObjectNode();
        if (approver.getId() != null) {
            approval.put("userId", approver.getId());
        }
        approval.put("name", approver.getName());
        approval.put("post", post);
        approval.put("email", approver.getEmail());
        approval.put("approvedAt", LocalDateTime.now().toString());
        approvals.set(post, approval);
        values.set("administrativeApprovals", approvals);
    }

    private String mergeAttachmentMetadata(String existingJson, String incomingJson) {
        String combined;
        if (incomingJson == null || incomingJson.isBlank()) {
            combined = existingJson;
        } else if (existingJson == null || existingJson.isBlank() || "[]".equals(existingJson.trim())) {
            combined = incomingJson;
        } else {
            ObjectMapper mapper = new ObjectMapper();
            try {
                com.fasterxml.jackson.databind.node.ArrayNode merged = mapper.createArrayNode();
                com.fasterxml.jackson.databind.JsonNode existing = mapper.readTree(existingJson);
                com.fasterxml.jackson.databind.JsonNode incoming = mapper.readTree(incomingJson);
                if (existing.isArray()) {
                    existing.forEach(merged::add);
                }
                if (incoming.isArray()) {
                    incoming.forEach(merged::add);
                }
                combined = mapper.writeValueAsString(merged);
            } catch (Exception e) {
                combined = incomingJson;
            }
        }
        return deduplicateAttachmentMetadataJson(combined);
    }

    private User resolveUser(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return userRepository.findByEmail(email).orElse(null);
    }

    private AdministrativePayload prepareAdministrativePayload(Submission submission, User caller, String incomingValuesData,
                                                               String incomingTablesData, String effectiveAttachments,
                                                               boolean submittingContribution) {
        if (!isAdministrativeSectionUser(caller, submission)) {
            return new AdministrativePayload(
                    incomingValuesData != null ? incomingValuesData : submission.getValuesData(),
                    incomingTablesData != null ? incomingTablesData : submission.getTablesData(),
                    false,
                    false
            );
        }

        String post = canonicalAdministrativePost(caller.getPost());
        if (post == null) {
            throw new SecurityException("Administrative post is required");
        }

        java.util.Set<String> ownedSections = ownedAdministrativeSections(post);
        ObjectMapper mapper = new ObjectMapper();
        try {
            com.fasterxml.jackson.databind.node.ObjectNode existingValues = objectNodeOrEmpty(mapper, submission.getValuesData());
            boolean alreadySubmitted = isAdministrativePostSubmitted(existingValues, post);
            if (alreadySubmitted && hasOwnedAdministrativeChanges(existingValues, incomingValuesData, this::classifyAdministrativeValueSection, ownedSections)) {
                throw new SecurityException("This administrative section has already been submitted");
            }
            if (alreadySubmitted && hasOwnedAdministrativeChanges(objectNodeOrEmpty(mapper, submission.getTablesData()), incomingTablesData, this::classifyAdministrativeTableSection, ownedSections)) {
                throw new SecurityException("This administrative section has already been submitted");
            }

            com.fasterxml.jackson.databind.node.ObjectNode mergedValues = mergeAdministrativeJson(
                    mapper,
                    submission.getValuesData(),
                    incomingValuesData,
                    this::classifyAdministrativeValueSection,
                    ownedSections,
                    "valuesData"
            );
            com.fasterxml.jackson.databind.node.ObjectNode mergedTables = mergeAdministrativeJson(
                    mapper,
                    submission.getTablesData(),
                    incomingTablesData,
                    this::classifyAdministrativeTableSection,
                    ownedSections,
                    "tablesData"
            );

            com.fasterxml.jackson.databind.node.ObjectNode progress = administrativeProgressNode(mapper, mergedValues);
            if (submittingContribution) {
                progress.put(post, "SUBMITTED");
            }
            mergedValues.set("administrativeProgress", progress);
            boolean allSubmitted = ADMIN_POSTS.stream()
                    .allMatch(requiredPost -> "SUBMITTED".equalsIgnoreCase(progress.path(requiredPost).asText("DRAFT")));

            String mergedValuesJson = mapper.writeValueAsString(mergedValues);
            String mergedTablesJson = mapper.writeValueAsString(mergedTables);
            return new AdministrativePayload(mergedValuesJson, mergedTablesJson, true, allSubmitted);
        } catch (SecurityException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Administrative payload must be valid JSON", e);
        }
    }

    private boolean isAdministrativeSectionUser(User caller, Submission submission) {
        return caller != null
                && "administrative".equalsIgnoreCase(caller.getRole())
                && submission != null
                && "administrative".equalsIgnoreCase(submission.getAuditType());
    }

    private com.fasterxml.jackson.databind.node.ObjectNode objectNodeOrEmpty(ObjectMapper mapper, String json) throws java.io.IOException {
        if (json == null || json.isBlank()) {
            return mapper.createObjectNode();
        }
        com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(json);
        if (node == null || node.isNull()) {
            return mapper.createObjectNode();
        }
        if (!node.isObject()) {
            throw new IllegalArgumentException("Administrative payload must be a JSON object");
        }
        return (com.fasterxml.jackson.databind.node.ObjectNode) node.deepCopy();
    }

    private boolean isEffectivelyEmpty(com.fasterxml.jackson.databind.JsonNode node) {
        if (node == null || node.isNull()) {
            return true;
        }
        if (node.isTextual() && node.asText().trim().isEmpty()) {
            return true;
        }
        if (node.isObject() && node.size() == 0) {
            return true;
        }
        if (node.isArray() && node.size() == 0) {
            return true;
        }
        return false;
    }

    private com.fasterxml.jackson.databind.node.ObjectNode mergeAdministrativeJson(ObjectMapper mapper, String existingJson,
                                                                                  String incomingJson,
                                                                                  java.util.function.Function<String, String> sectionClassifier,
                                                                                  java.util.Set<String> ownedSections,
                                                                                  String payloadName) throws java.io.IOException {
        com.fasterxml.jackson.databind.node.ObjectNode merged = objectNodeOrEmpty(mapper, existingJson);
        if (incomingJson == null) {
            return merged;
        }
        com.fasterxml.jackson.databind.node.ObjectNode incoming = objectNodeOrEmpty(mapper, incomingJson);
        incoming.fields().forEachRemaining(entry -> {
            if ("administrativeProgress".equals(entry.getKey()) || "administrativeApprovals".equals(entry.getKey())) {
                return;
            }
            String section = sectionClassifier.apply(entry.getKey());
            if (ownedSections.contains(section)) {
                merged.set(entry.getKey(), entry.getValue());
                return;
            }
            com.fasterxml.jackson.databind.JsonNode existingValue = merged.get(entry.getKey());
            if (existingValue != null && existingValue.equals(entry.getValue())) {
                return;
            }
            if (isEffectivelyEmpty(existingValue) && isEffectivelyEmpty(entry.getValue())) {
                return;
            }
            throw new SecurityException("Unauthorized " + payloadName + " modification for section " + section);
        });
        return merged;
    }

    private boolean hasOwnedAdministrativeChanges(com.fasterxml.jackson.databind.node.ObjectNode existing, String incomingJson,
                                                  java.util.function.Function<String, String> sectionClassifier,
                                                  java.util.Set<String> ownedSections) throws java.io.IOException {
        if (incomingJson == null || incomingJson.isBlank()) {
            return false;
        }
        ObjectMapper mapper = new ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode incoming = objectNodeOrEmpty(mapper, incomingJson);
        java.util.Iterator<Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = incoming.fields();
        while (fields.hasNext()) {
            Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> entry = fields.next();
            if ("administrativeProgress".equals(entry.getKey()) || "administrativeApprovals".equals(entry.getKey())) {
                continue;
            }
            if (!ownedSections.contains(sectionClassifier.apply(entry.getKey()))) {
                continue;
            }
            com.fasterxml.jackson.databind.JsonNode existingValue = existing.get(entry.getKey());
            if (existingValue == null || !existingValue.equals(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    private com.fasterxml.jackson.databind.node.ObjectNode administrativeProgressNode(ObjectMapper mapper,
                                                                                     com.fasterxml.jackson.databind.node.ObjectNode values) {
        com.fasterxml.jackson.databind.JsonNode existing = values.get("administrativeProgress");
        com.fasterxml.jackson.databind.node.ObjectNode progress = existing != null && existing.isObject()
                ? (com.fasterxml.jackson.databind.node.ObjectNode) existing.deepCopy()
                : mapper.createObjectNode();
        ADMIN_POSTS.forEach(post -> {
            if (!progress.has(post)) {
                progress.put(post, "DRAFT");
            }
        });
        return progress;
    }

    private boolean isAdministrativePostSubmitted(com.fasterxml.jackson.databind.node.ObjectNode values, String post) {
        com.fasterxml.jackson.databind.JsonNode progress = values.get("administrativeProgress");
        return progress != null && "SUBMITTED".equalsIgnoreCase(progress.path(post).asText());
    }

    private java.util.Set<String> ownedAdministrativeSections(String post) {
        return switch (post) {
            case "registrar" -> java.util.Set.of("A", "C");
            case "hr" -> java.util.Set.of("B");
            case "dean-student-welfare" -> java.util.Set.of("D");
            case "dean-placement" -> java.util.Set.of("E");
            default -> throw new SecurityException("Invalid administrative post");
        };
    }

    private String classifyAdministrativeTableSection(String key) {
        String normalized = normalizeJsonKey(key);
        if (normalized.contains("faculty") || normalized.contains("staff")) {
            return "B";
        }
        if (normalized.contains("statutory") || normalized.contains("infrastructure") || normalized.contains("library")
                || normalized.contains("eresource") || normalized.contains("it") || normalized.contains("sportsfacilities")
                || normalized.contains("divyangajan") || normalized.contains("researchresource")) {
            return "C";
        }
        if (normalized.contains("hackathon") || normalized.contains("cultural") || normalized.contains("sportsactivities")
                || normalized.contains("community") || normalized.contains("adminstudentawards")
                || normalized.contains("awardsprizesrecognitions")) {
            return "D";
        }
        if (normalized.contains("trainingactivities") || normalized.contains("industrycollaborations")
                || normalized.contains("placement")) {
            return "E";
        }
        return "A";
    }

    private String classifyAdministrativeValueSection(String key) {
        String normalized = normalizeJsonKey(key);
        if (normalized.contains("partb") || normalized.contains("hr") || normalized.contains("faculty") || normalized.contains("staff")) {
            return "B";
        }
        if (normalized.contains("partc") || normalized.contains("infrastructure") || normalized.contains("statutory")
                || normalized.contains("library") || normalized.contains("researchresource")) {
            return "C";
        }
        if (normalized.contains("partd") || normalized.contains("hackathon") || normalized.contains("cultural")
                || normalized.contains("sports") || normalized.contains("community") || normalized.contains("award")
                || normalized.contains("recognition")) {
            return "D";
        }
        if (normalized.contains("parte") || normalized.contains("placement") || normalized.contains("parteschools")
                || normalized.contains("trainingactivities") || normalized.contains("industrycollaboration")) {
            return "E";
        }
        return "A";
    }

    public String canonicalAdministrativePost(String post) {
        if (post == null || post.isBlank()) {
            return null;
        }
        String normalized = post.trim().toLowerCase().replace("_", "-");
        normalized = normalized.replaceAll("\\s+", "-");
        return switch (normalized) {
            case "registrar" -> "registrar";
            case "hr", "human-resources", "human-resource" -> "hr";
            case "dsw", "student-welfare", "dean-student-welfare", "dean-of-student-welfare" -> "dean-student-welfare";
            case "dean-placement", "placement", "dean-of-placement" -> "dean-placement";
            default -> normalized;
        };
    }

    private record AdministrativePayload(String valuesData, String tablesData, boolean administrativePartial,
                                         boolean allAdministrativePostsSubmitted) {
    }

    private String deduplicateAttachmentMetadataJson(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(json);
            if (!root.isArray()) {
                return json;
            }

            java.util.Set<String> seen = new java.util.HashSet<>();
            com.fasterxml.jackson.databind.node.ArrayNode deduped = mapper.createArrayNode();
            for (com.fasterxml.jackson.databind.JsonNode item : root) {
                String key = attachmentDedupeKey(item);
                if (key == null || seen.add(key)) {
                    deduped.add(item);
                } else {
                    System.err.println("Skipping duplicate attachment metadata: " + key);
                }
            }
            return mapper.writeValueAsString(deduped);
        } catch (Exception e) {
            return json;
        }
    }

    private String prepareTablesDataUpdate(Submission submission, String tablesData, String effectiveAttachments) {
        validateTableAttachmentMetadata(tablesData);
        cleanupRemovedTableAttachments(submission, tablesData, effectiveAttachments);
        return tablesData;
    }

    private void validateTableAttachmentMetadata(String tablesData) {
        if (tablesData == null || tablesData.isBlank()) {
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            validateTableAttachmentMetadata(mapper.readTree(tablesData), null);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("tablesData must be valid JSON");
        }
    }

    private void validateTableAttachmentMetadata(com.fasterxml.jackson.databind.JsonNode node, String fieldName) {
        if (node == null || node.isNull()) {
            return;
        }
        boolean attachmentContext = normalizeJsonKey(fieldName).contains("attachment");
        if (node.isObject()) {
            if (attachmentContext || hasAttachmentUrlField(node)) {
                validateAttachmentObject(node);
            }
            node.fields().forEachRemaining(entry -> validateTableAttachmentMetadata(entry.getValue(), entry.getKey()));
        } else if (node.isArray()) {
            node.forEach(item -> validateTableAttachmentMetadata(item, fieldName));
        }
    }

    private void validateAttachmentObject(com.fasterxml.jackson.databind.JsonNode node) {
        String url = textField(node, "url", "fileUrl", "downloadUrl");
        if (url == null) {
            throw new IllegalArgumentException("Attachment URL is required");
        }
        if (!isAllowedAttachmentUrl(url)) {
            throw new IllegalArgumentException("Invalid attachment storage URL");
        }

        String fileName = textField(node, "fileName", "name");
        if (fileName == null) {
            fileName = fileNameFromUrl(url);
        }
        String sanitized = sanitizeAttachmentFilename(fileName);
        if (!sanitized.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF attachments are allowed");
        }

        String size = textField(node, "size", "fileSize");
        if (size != null) {
            try {
                long parsedSize = Long.parseLong(size);
                if (parsedSize > AttachmentService.MAX_PDF_SIZE_BYTES) {
                    throw new IllegalArgumentException("Attachment file size exceeds maximum limit of 10MB");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Attachment file size must be numeric");
            }
        }
    }

    private void cleanupRemovedTableAttachments(Submission submission, String newTablesData, String effectiveAttachments) {
        java.util.Set<String> oldUrls = new java.util.HashSet<>();
        collectAttachmentUrls(submission.getTablesData(), oldUrls);
        if (oldUrls.isEmpty()) {
            return;
        }

        java.util.Set<String> stillReferenced = new java.util.HashSet<>();
        collectAttachmentUrls(newTablesData, stillReferenced);
        collectAttachmentUrls(submission.getValuesData(), stillReferenced);
        collectAttachmentUrls(effectiveAttachments, stillReferenced);

        oldUrls.forEach(url -> {
            if (stillReferenced.contains(url)) {
                return;
            }
            try {
                attachmentService.deleteFile(url);
            } catch (Exception e) {
                System.err.println("Unable to delete removed table attachment " + url + ": " + e.getMessage());
            }
        });
    }

    private boolean hasAttachmentUrlField(com.fasterxml.jackson.databind.JsonNode node) {
        return textField(node, "url", "fileUrl", "downloadUrl") != null;
    }

    private boolean isAllowedAttachmentUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        if (url.startsWith("/uploads/")) {
            return true;
        }
        try {
            java.net.URI uri = java.net.URI.create(url);
            String host = uri.getHost();
            return host != null && host.toLowerCase().contains("storage.googleapis.com")
                    && uri.getPath() != null && !uri.getPath().isBlank();
        } catch (Exception e) {
            return false;
        }
    }

    private String fileNameFromUrl(String url) {
        String normalized = url == null ? "" : url.replace("\\", "/");
        try {
            java.net.URI uri = java.net.URI.create(normalized);
            if (uri.getPath() != null && !uri.getPath().isBlank()) {
                normalized = uri.getPath();
            }
        } catch (Exception ignored) {
            // Fall back to the raw URL text.
        }
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }

    private String sanitizeAttachmentFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "attachment.pdf";
        }
        filename = filename.replace("\\", "/");
        int lastSlash = filename.lastIndexOf('/');
        String base = lastSlash >= 0 ? filename.substring(lastSlash + 1) : filename;
        base = base.replace("..", "_");
        String clean = base.replaceAll("[^A-Za-z0-9._-]", "_");
        return clean.isBlank() ? "attachment.pdf" : clean;
    }

    private void collectAttachmentUrls(String json, java.util.Set<String> urls) {
        if (json == null || json.isBlank()) {
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            collectAttachmentUrls(mapper.readTree(json), urls);
        } catch (Exception ignored) {
            // Ignore invalid JSON for cleanup.
        }
    }

    private void collectAttachmentUrls(com.fasterxml.jackson.databind.JsonNode node, java.util.Set<String> urls) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            String url = textField(node, "url", "fileUrl", "downloadUrl");
            if (url != null) {
                urls.add(normalizeUrl(url));
            }
            node.fields().forEachRemaining(entry -> collectAttachmentUrls(entry.getValue(), urls));
        } else if (node.isArray()) {
            node.forEach(item -> collectAttachmentUrls(item, urls));
        }
    }

    private String attachmentDedupeKey(com.fasterxml.jackson.databind.JsonNode item) {
        if (item == null || !item.isObject()) {
            return null;
        }
        String id = textField(item, "id", "attachmentId", "databaseId");
        if (id != null) {
            return "id:" + id;
        }
        String objectKey = textField(item, "objectKey", "storageObjectKey", "storageKey", "key");
        if (objectKey != null) {
            return "key:" + normalizeUrl(objectKey);
        }
        String url = textField(item, "url", "fileUrl", "downloadUrl");
        if (url != null) {
            return "url:" + normalizeUrl(url);
        }
        String checksum = textField(item, "checksum", "sha256", "md5");
        if (checksum != null) {
            return "checksum:" + checksum.toLowerCase();
        }
        String fileName = textField(item, "fileName", "name");
        String size = textField(item, "size", "fileSize");
        if (fileName != null && size != null) {
            return "name-size:" + fileName.trim().toLowerCase() + ":" + size.trim();
        }
        return null;
    }

    private String textField(com.fasterxml.jackson.databind.JsonNode node, String... names) {
        for (String name : names) {
            com.fasterxml.jackson.databind.JsonNode value = node.get(name);
            if (value != null && !value.isNull()) {
                String text = value.asText(null);
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private String normalizeUrl(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replace("\\", "/");
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.toLowerCase();
    }

    private String toAuditCycle(String academicYear) {
        if (academicYear == null || !academicYear.matches("\\d{4}-\\d{4}")) {
            return academicYear;
        }
        return academicYear.substring(0, 4) + "-" + academicYear.substring(7);
    }

    private String toAcademicYear(String value) {
        if (value != null && value.matches("\\d{4}-\\d{2}")) {
            return value.substring(0, 5) + value.substring(0, 2) + value.substring(5);
        }
        return value;
    }

    private Map<String, Object> toVersionHistoryResponse(Submission submission) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", submission.getId());
        response.put("version", submission.getVersion());
        response.put("academicYear", submission.getAcademicYear() != null ? submission.getAcademicYear() : submission.getAuditCycle());
        response.put("auditCycle", submission.getAuditCycle());
        response.put("schoolGroup", submission.getSchoolGroup());
        response.put("reportCategory", submission.getReportCategory());
        response.put("status", submission.getStatus());
        response.put("valuesData", submission.getValuesData());
        response.put("tablesData", submission.getTablesData());
        response.put("attachments", submission.getAttachments());
        response.put("auditorReviewedBy", submission.getAuditorReviewedBy());
        response.put("auditorReviewedOn", submission.getAuditorReviewedOn());
        response.put("approvedByName", submission.getApprovedByName());
        response.put("approvedAt", submission.getApprovedAt());
        return response;
    }

    private String clearCurrentCycleReviewData(String json, String auditType) {
        if (json == null || json.isBlank() || "{}".equals(json.trim())) {
            return json;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(json);
            if (!root.isObject()) {
                return json;
            }

            removeCurrentCycleReviewFields((com.fasterxml.jackson.databind.node.ObjectNode) root, auditType);
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            return json;
        }
    }

    private void removeCurrentCycleReviewFields(com.fasterxml.jackson.databind.node.ObjectNode node, String auditType) {
        List<String> removeKeys = new java.util.ArrayList<>();
        node.fields().forEachRemaining(entry -> {
            String normalizedKey = normalizeJsonKey(entry.getKey());
            if (shouldRemoveForNextCycle(normalizedKey, auditType)) {
                removeKeys.add(entry.getKey());
                return;
            }
            if (entry.getValue().isObject()) {
                removeCurrentCycleReviewFields((com.fasterxml.jackson.databind.node.ObjectNode) entry.getValue(), auditType);
            }
        });
        removeKeys.forEach(node::remove);
    }

    private boolean shouldRemoveForNextCycle(String normalizedKey, String auditType) {
        if (normalizedKey.contains("auditsignoff") || normalizedKey.contains("approved")
                || normalizedKey.contains("approval") || normalizedKey.contains("remark")) {
            return true;
        }
        if ("academic".equalsIgnoreCase(auditType)) {
            return normalizedKey.startsWith("parte") || normalizedKey.contains("parte");
        }
        if ("administrative".equalsIgnoreCase(auditType)) {
            return normalizedKey.startsWith("partf") || normalizedKey.contains("partf");
        }
        return false;
    }

    private String normalizeJsonKey(String value) {
        return value == null ? "" : value.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
