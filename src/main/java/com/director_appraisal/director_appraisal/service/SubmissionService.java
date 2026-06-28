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
    private static final List<String> EDITABLE_CYCLE_STATUSES = List.of("DRAFT", "SUBMITTED");

    private final SubmissionRepository submissionRepository;
    private final SnapshotRepository snapshotRepository;
    private final UserRepository userRepository;
    private final TableDataPromotionService tableDataPromotionService;
    private final SubmissionAuditorAssignmentRepository auditorAssignmentRepository;
    private final AcademicYearService academicYearService;
    private final UserAdministrativePostRepository userAdministrativePostRepository;

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
        submission.setValuesData(valuesData);
        submission.setTablesData(tablesData);
        submission.setAttachments(deduplicateAttachmentMetadataJson(attachments));
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
        submission.setValuesData(valuesData);
        submission.setTablesData(tablesData);
        submission.setAttachments(deduplicateAttachmentMetadataJson(attachments));
        submission.setStatus("SUBMITTED");
        submission.setSubmittedAt(LocalDateTime.now());
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
        submission.setValuesData(valuesData);
        submission.setTablesData(tablesData);
        submission.setAttachments(deduplicateAttachmentMetadataJson(attachments));
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
            throw new IllegalArgumentException("Send back workflow is disabled.");
        }
        if (!List.of(STATUS_APPROVED_LEGACY, STATUS_FINAL, "UNDER_REVIEW").contains(requestedStatus)) {
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
        if (valuesData != null) submission.setValuesData(valuesData);
        if (tablesData != null) submission.setTablesData(tablesData);
        if (attachments != null) submission.setAttachments(deduplicateAttachmentMetadataJson(attachments));
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

        if (!isOwner && !isIqac && !isAssignedAuditor) {
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
                throw new IllegalArgumentException("Send back workflow is disabled.");
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

        if (valuesData != null) submission.setValuesData(valuesData);
        if (tablesData != null) submission.setTablesData(tablesData);
        if (attachments != null) submission.setAttachments(deduplicateAttachmentMetadataJson(attachments));

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
