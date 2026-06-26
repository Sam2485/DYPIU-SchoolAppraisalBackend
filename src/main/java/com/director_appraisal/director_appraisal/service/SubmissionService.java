package com.director_appraisal.director_appraisal.service;

import com.director_appraisal.director_appraisal.model.Snapshot;
import com.director_appraisal.director_appraisal.model.Submission;
import com.director_appraisal.director_appraisal.model.User;
import com.director_appraisal.director_appraisal.repository.SnapshotRepository;
import com.director_appraisal.director_appraisal.repository.SubmissionRepository;
import com.director_appraisal.director_appraisal.repository.UserRepository;
import com.director_appraisal.director_appraisal.util.SchoolUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final UserRepository userRepository;

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
        
        // Updates can only occur before final approval or during draft/submitted/sent-back status
        String statusUpper = submission.getStatus().toUpperCase();
        if (List.of("UNDER_REVIEW", "AUDITOR_COMPLETED", "APPROVED").contains(statusUpper)) {
            throw new IllegalStateException("Cannot edit submission when status is " + statusUpper);
        }

        submission.setSchool(SchoolUtils.canonicalizeSchool(school));
        submission.setSubmittedBy(submittedBy);
        submission.setValuesData(valuesData);
        submission.setTablesData(tablesData);
        submission.setAttachments(attachments);
        submission.setVersion(submission.getVersion() + 1);

        Submission saved = submissionRepository.save(submission);
        createDraftSnapshot(saved);
        return saved;
    }

    @Transactional
    public Submission submitForm(String email, String auditType, String school, String submittedBy, 
                                 String valuesData, String tablesData, String attachments) {
        Submission submission = getOrCreateDraft(email, auditType);

        String statusUpper = submission.getStatus().toUpperCase();
        if (List.of("UNDER_REVIEW", "AUDITOR_COMPLETED", "APPROVED").contains(statusUpper)) {
            throw new IllegalStateException("Cannot edit submission when status is " + statusUpper);
        }

        submission.setSchool(SchoolUtils.canonicalizeSchool(school));
        submission.setSubmittedBy(submittedBy);
        submission.setValuesData(valuesData);
        submission.setTablesData(tablesData);
        submission.setAttachments(attachments);
        submission.setStatus("SUBMITTED");
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setVersion(submission.getVersion() + 1);

        Submission saved = submissionRepository.save(submission);
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
        if (List.of("UNDER_REVIEW", "AUDITOR_COMPLETED", "APPROVED").contains(statusUpper)) {
            throw new IllegalStateException("Cannot edit submission when status is " + statusUpper);
        }

        submission.setSchool(SchoolUtils.canonicalizeSchool(school));
        submission.setSubmittedBy(submittedBy);
        submission.setValuesData(valuesData);
        submission.setTablesData(tablesData);
        submission.setAttachments(attachments);
        submission.setVersion(submission.getVersion() + 1);

        Submission saved = submissionRepository.save(submission);
        createDraftSnapshot(saved);
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

        if (List.of("APPROVED", "SENT_BACK").contains(status.toUpperCase())) {
            if (!"AUDITOR_COMPLETED".equalsIgnoreCase(submission.getStatus())) {
                throw new IllegalStateException("Form can only be approved or sent back after the audit has been completed by an auditor");
            }
        }

        submission.setStatus(status.toUpperCase());
        submission.setRemarks(remarks);
        submission.setReviewedBy(reviewerName);
        submission.setReviewedAt(LocalDateTime.now());
        submission.setVersion(submission.getVersion() + 1);

        Submission saved = submissionRepository.save(submission);
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

    private void createDraftSnapshot(Submission submission) {
        String status = submission.getStatus();
        if (status == null) {
            return;
        }

        String statusUpper = status.toUpperCase();
        if (!List.of("DRAFT", "SENT_BACK").contains(statusUpper)) {
            return;
        }

        createSnapshot(submission);
    }

    public boolean isAuditorAssigned(User auditor, Submission submission) {
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
                return submitter.getPost() != null && submitter.getPost().equalsIgnoreCase(auditor.getPost());
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
                            return auditor.getPost() != null && auditor.getPost().equalsIgnoreCase(submitter.getPost());
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
            return submissionRepository.findByStatusIn(List.of("SUBMITTED", "UNDER_REVIEW", "AUDITOR_COMPLETED", "APPROVED", "SENT_BACK"));
        } else if ("vice-chancellor".equals(role)) {
            return submissionRepository.findByStatusIn(List.of("AUDITOR_COMPLETED", "APPROVED"));
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
        if ("APPROVED".equals(currentStatus)) {
            throw new IllegalStateException("Cannot edit an approved submission");
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
                submission.setStatus("UNDER_REVIEW");
            } else if (upperStatus.equals("SUBMITTED")) {
                submission.setStatus("SUBMITTED");
            } else if (upperStatus.equals("DRAFT")) {
                submission.setStatus("DRAFT");
            }
        }

        if (isIqac && forwardedAuditorType != null && !forwardedAuditorType.isBlank()) {
            populateForwardingAuditors(submission, forwardedAuditorType);
        }

        if (isIqac) {
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
        if (attachments != null) submission.setAttachments(attachments);

        submission.setVersion(submission.getVersion() + 1);

        Submission saved = submissionRepository.save(submission);
        createDraftSnapshot(saved);
        return saved;
    }
}
