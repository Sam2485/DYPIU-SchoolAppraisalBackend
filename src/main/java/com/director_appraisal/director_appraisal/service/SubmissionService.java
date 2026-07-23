package com.director_appraisal.director_appraisal.service;

import com.director_appraisal.director_appraisal.exception.ConflictException;
import com.director_appraisal.director_appraisal.model.Snapshot;
import com.director_appraisal.director_appraisal.model.Submission;
import com.director_appraisal.director_appraisal.model.SubmissionAuditorAssignment;
import com.director_appraisal.director_appraisal.model.User;
import com.director_appraisal.director_appraisal.model.UserAdministrativePost;
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
    private static final List<String> IQAC_VISIBLE_STATUSES = List.of("SUBMITTED", "UNDER_REVIEW", "FORWARDED_TO_INTERNAL_AUDITOR", "INTERNAL_AUDITOR_COMPLETED", "FORWARDED_TO_EXTERNAL_AUDITOR", "AUDITOR_COMPLETED", "EXTERNAL_AUDITOR_COMPLETED", STATUS_APPROVED_LEGACY, STATUS_FINAL);
    private static final List<String> VC_VISIBLE_STATUSES = List.of("AUDITOR_COMPLETED", "EXTERNAL_AUDITOR_COMPLETED", STATUS_APPROVED_LEGACY, STATUS_FINAL);
    private static final List<String> NORMALIZED_TABLE_STATUSES = List.of("SUBMITTED", "UNDER_REVIEW", "AUDITOR_COMPLETED", STATUS_APPROVED_LEGACY, STATUS_FINAL);
    private static final List<String> EDITABLE_CYCLE_STATUSES = List.of("DRAFT", "SUBMITTED", "SENT_BACK");
    private static final List<String> ADMIN_POSTS = List.of("registrar", "hr", "dean-student-welfare", "dean-placement");
    private static final String SHARED_ADMINISTRATIVE_EMAIL = "administrative.shared@dypiu.ac.in";
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SubmissionService.class);

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
        Submission submission = getOrCreateSharedAdministrativeDraftForCycle(academicYear);
        
        // Concurrency safety: row-level lock
        Submission locked = submissionRepository.findByIdForUpdate(submission.getId()).orElse(submission);
        
        ObjectMapper mapper = new ObjectMapper();
        boolean modified = false;
        try {
            com.fasterxml.jackson.databind.node.ObjectNode values = objectNodeOrEmpty(mapper, locked.getValuesData());
            com.fasterxml.jackson.databind.node.ObjectNode tables = objectNodeOrEmpty(mapper, locked.getTablesData());
            
            com.fasterxml.jackson.databind.JsonNode statusNode = values.get("__administrativeSubmissionStatus");
            if (statusNode != null && statusNode.isObject()) {
                com.fasterxml.jackson.databind.node.ObjectNode statusObj = (com.fasterxml.jackson.databind.node.ObjectNode) statusNode;
                java.util.Set<String> postsToClear = new java.util.LinkedHashSet<>();
                
                statusObj.fields().forEachRemaining(entry -> {
                    String post = entry.getKey();
                    com.fasterxml.jackson.databind.JsonNode postNode = entry.getValue();
                    if (postNode != null && postNode.path("submitted").asBoolean()) {
                        String email = postNode.path("email").asText(null);
                        Long storedUserId = postNode.has("userId") ? postNode.get("userId").asLong() : null;
                        
                        boolean isActive = false;
                        if (email != null && !email.isBlank()) {
                            Optional<User> uOpt = userRepository.findByEmail(email.trim().toLowerCase())
                                    .filter(u -> !Boolean.TRUE.equals(u.getDeleted()));
                            if (uOpt.isPresent()) {
                                User activeUser = uOpt.get();
                                if (storedUserId == null || storedUserId.equals(activeUser.getId())) {
                                    isActive = true;
                                }
                            }
                        }
                        
                        if (!isActive) {
                            postsToClear.add(post);
                        }
                    }
                });
                
                if (!postsToClear.isEmpty()) {
                    java.util.Set<String> sections = new java.util.LinkedHashSet<>();
                    postsToClear.forEach(post -> sections.addAll(ownedAdministrativeSections(post)));
                    
                    removeAdministrativeOwnedFields(values, this::classifyAdministrativeValueSection, sections);
                    removeAdministrativeOwnedFields(tables, this::classifyAdministrativeTableSection, sections);
                    resetAdministrativeProgress(values, postsToClear);
                    removeAdministrativeApprovals(values, postsToClear);
                    removeAdministrativeSubmissionStatus(values, postsToClear);
                    locked.setSubmittedByDetails(removeAdministrativeSubmittedByDetails(mapper, locked.getSubmittedByDetails(), postsToClear));
                    
                    locked.setValuesData(mapper.writeValueAsString(values));
                    locked.setTablesData(mapper.writeValueAsString(tables));
                    locked.setAttachments(removeAdministrativeOwnedAttachments(mapper, locked.getAttachments(), sections));
                    
                    boolean allSubmitted = ADMIN_POSTS.stream()
                            .allMatch(requiredPost -> {
                                if (!isPostRequiredForAdministrativeWorkflow(requiredPost)) {
                                    return true;
                                }
                                String status = values.path("administrativeProgress").path(requiredPost).asText("DRAFT");
                                return "SUBMITTED".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status);
                            });
                    if (!allSubmitted && !"DRAFT".equalsIgnoreCase(locked.getStatus())) {
                        locked.setStatus("DRAFT");
                        locked.setSubmittedAt(null);
                    }
                    
                    modified = true;
                }
            }
        } catch (Exception e) {
            log.error("Failed to dynamically clean up deleted administrative contributions", e);
        }
        
        if (modified) {
            return submissionRepository.save(locked);
        }
        return locked;
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

            com.fasterxml.jackson.databind.JsonNode existingStatusNode = valuesNode.get("__administrativeSubmissionStatus");
            com.fasterxml.jackson.databind.node.ObjectNode statusNode = (existingStatusNode != null && existingStatusNode.isObject())
                    ? (com.fasterxml.jackson.databind.node.ObjectNode) existingStatusNode
                    : mapper.createObjectNode();
            com.fasterxml.jackson.databind.node.ObjectNode postStatus = mapper.createObjectNode();
            postStatus.put("submitted", true);
            postStatus.put("submittedAt", LocalDateTime.now().toString());
            postStatus.put("name", caller.getName());
            postStatus.put("email", caller.getEmail());
            postStatus.put("userId", caller.getId());
            statusNode.set(post, postStatus);
            valuesNode.set("__administrativeSubmissionStatus", statusNode);

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
                        if (!isPostRequiredForAdministrativeWorkflow(requiredPost)) {
                            return true;
                        }
                        String st = progress.path(requiredPost).asText("DRAFT");
                        return "SUBMITTED".equalsIgnoreCase(st) || "APPROVED".equalsIgnoreCase(st);
                    });

            if (allSubmitted) {
                lockedSubmission.setStatus("SUBMITTED");
                lockedSubmission.setSubmittedAt(LocalDateTime.now());
                if ("EXTERNAL".equalsIgnoreCase(lockedSubmission.getReportCategory()) || "external".equalsIgnoreCase(lockedSubmission.getForwardedAuditorType()) || (lockedSubmission.getVersion() != null && lockedSubmission.getVersion() > 1)) {
                    autoForwardToExternalAuditors(lockedSubmission);
                }
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

    @Transactional
    public void removeAdministrativeUserContribution(User user) {
        if (user == null || !"administrative".equalsIgnoreCase(user.getRole())) {
            return;
        }

        java.util.Set<String> posts = resolveAdministrativePosts(user);
        if (posts.isEmpty()) {
            return;
        }

        java.util.Set<String> sections = new java.util.LinkedHashSet<>();
        posts.forEach(post -> sections.addAll(ownedAdministrativeSections(post)));

        List<Submission> sharedForms = submissionRepository.findAllByEmailIgnoreCase(SHARED_ADMINISTRATIVE_EMAIL).stream()
                .filter(submission -> "administrative".equalsIgnoreCase(submission.getAuditType()))
                .toList();

        ObjectMapper mapper = new ObjectMapper();
        for (Submission sharedForm : sharedForms) {
            Submission locked = submissionRepository.findByIdForUpdate(sharedForm.getId()).orElse(sharedForm);
            try {
                com.fasterxml.jackson.databind.node.ObjectNode values = objectNodeOrEmpty(mapper, locked.getValuesData());
                com.fasterxml.jackson.databind.node.ObjectNode tables = objectNodeOrEmpty(mapper, locked.getTablesData());

                removeAdministrativeOwnedFields(values, this::classifyAdministrativeValueSection, sections);
                removeAdministrativeOwnedFields(tables, this::classifyAdministrativeTableSection, sections);
                resetAdministrativeProgress(values, posts);
                removeAdministrativeApprovals(values, posts);
                removeAdministrativeSubmissionStatus(values, posts);
                locked.setSubmittedByDetails(removeAdministrativeSubmittedByDetails(mapper, locked.getSubmittedByDetails(), posts));

                locked.setValuesData(mapper.writeValueAsString(values));
                locked.setTablesData(mapper.writeValueAsString(tables));
                locked.setAttachments(removeAdministrativeOwnedAttachments(mapper, locked.getAttachments(), sections));

                boolean allSubmitted = ADMIN_POSTS.stream()
                        .allMatch(requiredPost -> {
                            if (!isPostRequiredForAdministrativeWorkflow(requiredPost)) {
                                return true;
                            }
                            String status = values.path("administrativeProgress").path(requiredPost).asText("DRAFT");
                            return "SUBMITTED".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status);
                        });
                if (!allSubmitted && !"DRAFT".equalsIgnoreCase(locked.getStatus())) {
                    locked.setStatus("DRAFT");
                    locked.setSubmittedAt(null);
                }

                submissionRepository.save(locked);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to remove administrative user contribution", e);
            }
        }
    }

    @Transactional
    public void deleteUserSubmissionsAndAttachments(User user) {
        if (user == null) {
            return;
        }
        String email = user.getEmail() != null ? user.getEmail().trim() : null;

        List<Submission> submissions = new java.util.ArrayList<>();

        if (email != null && !email.isBlank()) {
            submissions.addAll(submissionRepository.findAllByEmailIgnoreCase(email));
        }

        for (Submission submission : submissions) {
            if (submission == null || submission.getId() == null) {
                continue;
            }

            Long rootId = resolveRootSubmissionId(submission);
            List<Submission> lineage = submissionRepository.findLineage(rootId);
            for (Submission target : lineage) {
                if (target == null || target.getId() == null) continue;

                // Delete physical files
                if (target.getAttachments() != null && !target.getAttachments().isBlank()) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(target.getAttachments());
                        if (root.isArray()) {
                            root.forEach(item -> {
                                String url = textField(item, "url", "fileUrl", "downloadUrl");
                                if (url != null) {
                                    try {
                                        attachmentService.deleteFile(url);
                                    } catch (Exception ignored) {}
                                }
                            });
                        }
                    } catch (Exception ignored) {}
                }

                try {
                    java.util.Set<String> urls = new java.util.HashSet<>();
                    collectAttachmentUrls(target.getTablesData(), urls);
                    collectAttachmentUrls(target.getValuesData(), urls);
                    for (String url : urls) {
                        try {
                            attachmentService.deleteFile(url);
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}

                snapshotRepository.deleteBySubmissionId(target.getId());
                auditorAssignmentRepository.deleteBySubmissionId(target.getId());
                submissionRepository.delete(target);
            }
        }

        if (email != null && !email.isBlank()) {
            attachmentService.deleteUserUploads(email);
        }
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
                        if (!isPostRequiredForAdministrativeWorkflow(post)) {
                            return true;
                        }
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
            Submission sub = editableCycle.get();
            populateAuditorProgressAndAssignments(sub);
            return sub;
        }

        Optional<Submission> latestCycle = submissionRepository.findFirstByEmailAndAuditTypeAndAcademicYearOrderByIdDesc(email, auditType, academicYear);
        if (latestCycle.isPresent()) {
            Submission sub = latestCycle.get();
            populateAuditorProgressAndAssignments(sub);
            return sub;
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
        Submission finalSaved = submissionRepository.save(saved);
        populateAuditorProgressAndAssignments(finalSaved);
        return finalSaved;
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
        Optional<Submission> opt = submissionRepository.findById(id);
        opt.ifPresent(this::populateAuditorProgressAndAssignments);
        return opt;
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
            if (!"AUDITOR_COMPLETED".equalsIgnoreCase(submission.getStatus())
                    && !"EXTERNAL_AUDITOR_COMPLETED".equalsIgnoreCase(submission.getStatus())
                    && !"INTERNAL_AUDITOR_COMPLETED".equalsIgnoreCase(submission.getStatus())) {
                throw new IllegalStateException("Form can only be approved after the audit has been completed by an auditor");
            }
        }

        if (isApprovalStatus(requestedStatus)) {
            validateReviewer(reviewer);
            System.out.println("[AUDIT_DEBUG] reviewSubmission: id=" + id + ", version=" + submission.getVersion() + ", forwardedAuditorType=" + submission.getForwardedAuditorType() + ", requestReportCategory=" + reportCategory);
            String expectedReportCategory = resolveExpectedReportCategoryForApproval(submission);
            System.out.println("[AUDIT_DEBUG] resolved expectedReportCategory: " + expectedReportCategory);
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
                    System.out.println("[AUDIT_DEBUG] Warning: requested version " + version + " does not match submission version " + submission.getVersion() + ". Using submission version.");
                } else {
                    submission.setVersion(version);
                }
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
                .forwardedAdministrativePosts(submission.getForwardedAdministrativePosts())
                .forwardedToAuditorPosts(submission.getForwardedToAuditorPosts())
                .auditorCorrectionRequested(submission.getAuditorCorrectionRequested())
                .correctionRequestedForAuditor(submission.getCorrectionRequestedForAuditor())
                .requiresAuditorResubmission(submission.getRequiresAuditorResubmission())
                .auditorCorrectionMessage(submission.getAuditorCorrectionMessage())
                .auditorCorrectionRequestedBy(submission.getAuditorCorrectionRequestedBy())
                .auditorCorrectionRequestedByRole(submission.getAuditorCorrectionRequestedByRole())
                .auditorCorrectionRequestedOn(submission.getAuditorCorrectionRequestedOn())
                .auditorResubmittedAt(submission.getAuditorResubmittedAt())
                .auditorReviewedByEmail(submission.getAuditorReviewedByEmail())
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
        boolean statusMatch = List.of("SUBMITTED", "UNDER_REVIEW", "AUDITOR_COMPLETED", "FORWARDED_TO_INTERNAL_AUDITOR", "FORWARDED_TO_EXTERNAL_AUDITOR").contains(submission.getStatus().toUpperCase());
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
            java.util.Set<String> auditorPosts = resolveAdministrativePosts(auditor);
            java.util.Set<String> submissionPosts = resolveSubmissionPostsForList(submission);
            return !submissionPosts.isEmpty() && hasPostOverlap(auditorPosts, submissionPosts);
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

        List<User> allAuditors = userRepository.findByAccountType("auditor").stream()
                .filter(u -> !Boolean.TRUE.equals(u.getDeleted()))
                .toList();
        
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

        List<Submission> allInDb = submissionRepository.findAll();
        System.out.println("[AUDIT_DEBUG] getAllSubmissionsForUser: user=" + user.getEmail() + ", role=" + role + ", totalInDb=" + allInDb.size());
        for (Submission s : allInDb) {
            System.out.println("[AUDIT_DEBUG]   Sub in DB: id=" + s.getId() + ", auditType=" + s.getAuditType() + ", status=" + s.getStatus() + ", email=" + s.getEmail());
        }

        // Auto-sync administrative draft submission statuses if all active posts submitted
        try {
            List<Submission> draftAdminForms = allInDb.stream()
                    .filter(sub -> "administrative".equalsIgnoreCase(sub.getAuditType()) && "DRAFT".equalsIgnoreCase(sub.getStatus()))
                    .toList();
            for (Submission draftAdmin : draftAdminForms) {
                ObjectMapper mapper = new ObjectMapper();
                com.fasterxml.jackson.databind.node.ObjectNode values = objectNodeOrEmpty(mapper, draftAdmin.getValuesData());
                com.fasterxml.jackson.databind.node.ObjectNode progress = administrativeProgressNode(mapper, values);
                System.out.println("[AUDIT_DEBUG]   draftAdmin id=" + draftAdmin.getId() + " progressNode: " + progress.toString());
                boolean allSubmitted = ADMIN_POSTS.stream()
                        .allMatch(requiredPost -> {
                            if (!isPostRequiredForAdministrativeWorkflow(requiredPost)) {
                                return true;
                            }
                            String st = progress.path(requiredPost).asText("DRAFT");
                            return "SUBMITTED".equalsIgnoreCase(st) || "APPROVED".equalsIgnoreCase(st);
                        });
                System.out.println("[AUDIT_DEBUG]   draftAdmin id=" + draftAdmin.getId() + " allSubmitted=" + allSubmitted);
                if (allSubmitted) {
                    draftAdmin.setStatus("SUBMITTED");
                    if (draftAdmin.getSubmittedAt() == null) {
                        draftAdmin.setSubmittedAt(LocalDateTime.now());
                    }
                    submissionRepository.save(draftAdmin);
                }
            }
        } catch (Exception e) {
            System.out.println("[AUDIT_DEBUG] Error in auto-sync: " + e.getMessage());
        }

        List<Submission> list;

        if ("iqac".equals(role)) {
            list = submissionRepository.findByStatusIn(IQAC_VISIBLE_STATUSES);
            System.out.println("[AUDIT_DEBUG] iqac list count: " + list.size());
        } else if ("vice-chancellor".equals(role)) {
            list = submissionRepository.findByStatusIn(VC_VISIBLE_STATUSES);
        } else if (role.contains("auditor") || "auditor".equalsIgnoreCase(user.getAccountType())) {
            List<Submission> allSubmissions = submissionRepository.findAll();
            list = allSubmissions.stream()
                    .filter(sub -> {
                        boolean matchesStatus = List.of("SUBMITTED", "UNDER_REVIEW", "AUDITOR_COMPLETED").contains(sub.getStatus().toUpperCase());
                        if (!matchesStatus) {
                            return false;
                        }
                        return isAuditorAssigned(user, sub) || isAuditorFallbackMatch(user, sub);
                    })
                    .toList();
        } else {
            list = List.of();
        }

        // Exclude active submissions where the submitting user is soft-deleted/inactive
        return list.stream()
                .filter(sub -> {
                    if ("APPROVED".equalsIgnoreCase(sub.getStatus()) || "FINAL".equalsIgnoreCase(sub.getStatus())) {
                        return true;
                    }
                    if ("administrative".equalsIgnoreCase(sub.getAuditType()) || SHARED_ADMINISTRATIVE_EMAIL.equalsIgnoreCase(sub.getEmail())) {
                        return true;
                    }
                    if (sub.getEmail() != null) {
                        Optional<User> submitter = userRepository.findByEmail(sub.getEmail().trim().toLowerCase());
                        if (submitter.isPresent() && Boolean.TRUE.equals(submitter.get().getDeleted())) {
                            return false;
                        }
                    }
                    return true;
                })
                .peek(this::populateAuditorProgressAndAssignments)
                .toList();
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
        return updateSubmission(id, caller, status, forwardedAuditorType, forwardedAuditCategory,
                requestForwardedToAuditorIds, requestForwardedToAuditorNames, requestForwardedToAuditorEmails,
                valuesData, tablesData, attachments, null, null,
                null, null, null, null, null, null, null, null, null);
    }

    @Transactional
    public Submission updateSubmission(Long id, User caller, String status, String forwardedAuditorType,
                                       String forwardedAuditCategory,
                                       List<Long> requestForwardedToAuditorIds,
                                       List<String> requestForwardedToAuditorNames,
                                       List<String> requestForwardedToAuditorEmails,
                                       String valuesData, String tablesData, String attachments,
                                       List<String> forwardedAdministrativePosts,
                                       List<String> forwardedToAuditorPosts) {
        return updateSubmission(id, caller, status, forwardedAuditorType, forwardedAuditCategory,
                requestForwardedToAuditorIds, requestForwardedToAuditorNames, requestForwardedToAuditorEmails,
                valuesData, tablesData, attachments, forwardedAdministrativePosts, forwardedToAuditorPosts,
                null, null, null, null, null, null, null, null, null);
    }

    @Transactional
    public Submission updateSubmission(Long id, User caller, String status, String forwardedAuditorType,
                                       String forwardedAuditCategory,
                                       List<Long> requestForwardedToAuditorIds,
                                       List<String> requestForwardedToAuditorNames,
                                       List<String> requestForwardedToAuditorEmails,
                                       String valuesData, String tablesData, String attachments,
                                       List<String> forwardedAdministrativePosts,
                                       List<String> forwardedToAuditorPosts,
                                       Boolean auditorCorrectionRequested,
                                       Boolean correctionRequestedForAuditor,
                                       Boolean requiresAuditorResubmission,
                                       String auditorCorrectionMessage,
                                       String auditorCorrectionRequestedBy,
                                       String auditorCorrectionRequestedByRole,
                                       String auditorCorrectionRequestedOn,
                                       String auditorResubmittedAt,
                                       String remarks) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + id));

        String callerRole = caller.getRole().toLowerCase();
        String callerEmail = caller.getEmail();

        boolean isOwner = submission.getEmail().equalsIgnoreCase(callerEmail);
        boolean isIqac = "iqac".equals(callerRole);
        boolean isVc = "vice-chancellor".equals(callerRole);
        boolean isAuditor = callerRole.contains("auditor") || "auditor".equalsIgnoreCase(caller.getAccountType());
        boolean isAssignedAuditor = isAuditor && (isAuditorAssigned(caller, submission) || isAuditorFallbackMatch(caller, submission));
        boolean isAdministrativeContributor = "administrative".equals(callerRole)
                && "administrative".equalsIgnoreCase(submission.getAuditType());

        if (!isOwner && !isIqac && !isVc && !isAssignedAuditor && !isAdministrativeContributor) {
            throw new IllegalStateException("You are not authorized to edit this submission");
        }

        if (isAssignedAuditor && "administrative".equalsIgnoreCase(submission.getAuditType())) {
            validateAuditorAccessToUpdates(submission, caller, valuesData, tablesData);
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

        boolean isCorrectionReturn = Boolean.TRUE.equals(auditorCorrectionRequested)
                || Boolean.TRUE.equals(correctionRequestedForAuditor)
                || Boolean.TRUE.equals(requiresAuditorResubmission);

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
                submission.setAuditorReviewedByEmail(caller.getEmail());
                submission.setAuditorReviewedByDesignation(caller.getDesignation());
                submission.setAuditorReviewedByRole(caller.getRole());
                submission.setAuditorReviewedOn(LocalDateTime.now());
                valuesData = injectAuditorSignOff(valuesData, caller);

                // Clear correction flags
                submission.setAuditorCorrectionRequested(false);
                submission.setCorrectionRequestedForAuditor(false);
                submission.setRequiresAuditorResubmission(false);
                if (auditorResubmittedAt != null) {
                    submission.setAuditorResubmittedAt(parseDateTime(auditorResubmittedAt));
                } else {
                    submission.setAuditorResubmittedAt(LocalDateTime.now());
                }
            } else if (upperStatus.equals("UNDER_REVIEW")) {
                if (!isIqac && !isAssignedAuditor) {
                    throw new IllegalStateException("Only IQAC can forward submissions for review");
                }
                if (isIqac) {
                    if (isCorrectionReturn) {
                        boolean auditorDeleted = false;
                        if (submission.getForwardedToAuditorId() != null) {
                            Optional<User> audOpt = userRepository.findById(submission.getForwardedToAuditorId());
                            if (audOpt.isPresent() && Boolean.TRUE.equals(audOpt.get().getDeleted())) {
                                auditorDeleted = true;
                            }
                        } else {
                            List<SubmissionAuditorAssignment> assignments = auditorAssignmentRepository.findBySubmissionId(submission.getId());
                            for (SubmissionAuditorAssignment ass : assignments) {
                                Optional<User> audOpt = userRepository.findById(ass.getAuditorId());
                                if (audOpt.isPresent() && Boolean.TRUE.equals(audOpt.get().getDeleted())) {
                                    auditorDeleted = true;
                                    break;
                                }
                            }
                        }
                        if (auditorDeleted) {
                            throw new IllegalStateException("Assigned auditor account is inactive/deleted. Please assign another auditor before returning for correction.");
                        }

                        submission.setAuditorCorrectionRequested(true);
                        submission.setCorrectionRequestedForAuditor(true);
                        submission.setRequiresAuditorResubmission(true);
                        submission.setAuditorCorrectionMessage(auditorCorrectionMessage != null ? auditorCorrectionMessage : remarks);
                        submission.setAuditorCorrectionRequestedBy(auditorCorrectionRequestedBy);
                        submission.setAuditorCorrectionRequestedByRole(auditorCorrectionRequestedByRole);
                        submission.setAuditorCorrectionRequestedOn(parseDateTime(auditorCorrectionRequestedOn));
                        if (remarks != null) {
                            submission.setRemarks(remarks);
                        }
                    } else {
                        System.out.println("[AUDIT_DEBUG] IQAC forwarding submission " + submission.getId() + " to auditors. AuditorType=" + forwardedAuditorType + ", ids=" + requestForwardedToAuditorIds + ", adminPosts=" + forwardedAdministrativePosts + ", auditorPosts=" + forwardedToAuditorPosts);
                        assignSelectedAuditorsForReview(
                                submission,
                                forwardedAuditorType,
                                requestForwardedToAuditorIds,
                                forwardedAdministrativePosts,
                                forwardedToAuditorPosts
                        );
                    }
                }
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
                if (remarks != null && !remarks.isBlank()) {
                    submission.setRemarks(remarks);
                }
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

        if (isIqac && !isCorrectionReturn && (status == null || !"UNDER_REVIEW".equalsIgnoreCase(status))) {
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
                if (forwardedAdministrativePosts != null) {
                    submission.setForwardedAdministrativePosts(mapper.writeValueAsString(forwardedAdministrativePosts));
                }
                if (forwardedToAuditorPosts != null) {
                    submission.setForwardedToAuditorPosts(mapper.writeValueAsString(forwardedToAuditorPosts));
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
        if ("SUBMITTED".equalsIgnoreCase(saved.getStatus()) && ("EXTERNAL".equalsIgnoreCase(saved.getReportCategory()) || "external".equalsIgnoreCase(saved.getForwardedAuditorType()) || (saved.getVersion() != null && saved.getVersion() > 1))) {
            autoForwardToExternalAuditors(saved);
        }
        persistDataForStatus(saved);
        return saved;
    }

    @Transactional
    public void autoForwardToExternalAuditors(Submission submission) {
        if (submission == null) return;
        
        List<User> externalAuditors = userRepository.findAll().stream()
                .filter(u -> !Boolean.TRUE.equals(u.getDeleted()))
                .filter(u -> {
                    String role = u.getRole() != null ? u.getRole().toLowerCase() : "";
                    String type = u.getAuditorType() != null ? u.getAuditorType().toLowerCase() : "";
                    String acct = u.getAccountType() != null ? u.getAccountType().toLowerCase() : "";
                    return role.contains("external") || "external".equalsIgnoreCase(type);
                })
                .toList();

        if ("academic".equalsIgnoreCase(submission.getAuditType())) {
            String school = submission.getSchool();
            if (school != null && !school.isBlank()) {
                externalAuditors = externalAuditors.stream()
                        .filter(u -> school.equalsIgnoreCase(u.getSchool()))
                        .toList();
            }
        }

        if (externalAuditors.isEmpty()) {
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        List<Long> ids = new java.util.ArrayList<>();
        List<String> names = new java.util.ArrayList<>();
        List<String> emails = new java.util.ArrayList<>();
        List<String> posts = new java.util.ArrayList<>();

        for (User auditor : externalAuditors) {
            ids.add(auditor.getId());
            names.add(auditor.getName());
            emails.add(auditor.getEmail());
            
            String post = "academic".equalsIgnoreCase(submission.getAuditType()) ? auditor.getSchool() : auditor.getPost();
            if (post != null) posts.add(post);

            List<SubmissionAuditorAssignment> existingAssignment = auditorAssignmentRepository
                    .findBySubmissionIdAndAuditorId(submission.getId(), auditor.getId());
            if (existingAssignment.isEmpty()) {
                SubmissionAuditorAssignment assignment = SubmissionAuditorAssignment.builder()
                        .submissionId(submission.getId())
                        .auditorId(auditor.getId())
                        .auditorName(auditor.getName())
                        .auditorEmail(auditor.getEmail())
                        .auditorType("external")
                        .category(submission.getAuditType())
                        .post(post)
                        .status("PENDING")
                        .assignedAt(LocalDateTime.now())
                        .build();
                auditorAssignmentRepository.save(assignment);
            }
        }

        try {
            submission.setForwardedToAuditorIds(mapper.writeValueAsString(ids));
            submission.setForwardedToAuditorNames(mapper.writeValueAsString(names));
            submission.setForwardedToAuditorEmails(mapper.writeValueAsString(emails));
            if (!posts.isEmpty()) {
                submission.setForwardedToAuditorPosts(mapper.writeValueAsString(posts));
            }
        } catch (Exception ignored) {}

        submission.setForwardedAuditorType("external");
        submission.setStatus("FORWARDED_TO_EXTERNAL_AUDITOR");
        submission.setForwardedAt(LocalDateTime.now());
        submissionRepository.save(submission);
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

        if (approved.getVersion() == null) {
            approved.setVersion(1);
        }

        if (!"EXTERNAL".equalsIgnoreCase(clean(nextAuditorType))) {
            throw new IllegalArgumentException("Next auditor type must be EXTERNAL");
        }

        if (Boolean.TRUE.equals(approved.getHasNextCycle()) || approved.getNextVersionId() != null) {
            throw new ConflictException("Next cycle already exists");
        }

        Long rootSubmissionId = resolveRootSubmissionId(approved);
        int expectedNextVersion = approved.getVersion() + 1;

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

        String nextValues = clearCurrentCycleReviewData(approved.getValuesData(), approved.getAuditType());
        String nextTables = clearCurrentCycleReviewData(approved.getTablesData(), approved.getAuditType());
        
        if ("administrative".equalsIgnoreCase(approved.getAuditType())) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                com.fasterxml.jackson.databind.node.ObjectNode valuesNode = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(nextValues);
                com.fasterxml.jackson.databind.node.ObjectNode progressNode = mapper.createObjectNode();
                progressNode.put("registrar", "DRAFT");
                progressNode.put("hr", "DRAFT");
                progressNode.put("dean-student-welfare", "DRAFT");
                progressNode.put("dean-placement", "DRAFT");
                valuesNode.set("administrativeProgress", progressNode);
                valuesNode.remove("__administrativeSubmissionStatus");
                nextValues = mapper.writeValueAsString(valuesNode);
            } catch (Exception ignored) {}
        }

        Submission next = Submission.builder()
                .email(approved.getEmail())
                .auditType(approved.getAuditType())
                .school(approved.getSchool())
                .submittedBy(approved.getSubmittedBy())
                .status("DRAFT")
                .valuesData(nextValues)
                .tablesData(nextTables)
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

    private void assignSelectedAuditorsForReview(Submission submission, String forwardedAuditorType, List<Long> selectedAuditorIds, List<String> forwardedAdministrativePosts, List<String> forwardedToAuditorPosts) {
        if (submission.getId() == null) {
            throw new IllegalStateException("Submission must be saved before auditor assignment");
        }
        if (auditorAssignmentRepository.existsBySubmissionId(submission.getId())) {
            auditorAssignmentRepository.deleteBySubmissionId(submission.getId());
            auditorAssignmentRepository.flush();
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

        if ("administrative".equalsIgnoreCase(submission.getAuditType())) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                com.fasterxml.jackson.databind.node.ObjectNode valuesNode = objectNodeOrEmpty(mapper, submission.getValuesData());
                com.fasterxml.jackson.databind.JsonNode progress = valuesNode.get("administrativeProgress");
                boolean allSubmitted = ADMIN_POSTS.stream()
                        .allMatch(post -> {
                            if (!isPostRequiredForAdministrativeWorkflow(post)) {
                                return true;
                            }
                            if (progress == null || !progress.isObject()) {
                                return false;
                            }
                            String st = progress.path(post).asText("DRAFT");
                            return "APPROVED".equalsIgnoreCase(st) || "SUBMITTED".equalsIgnoreCase(st);
                        });
                if (!allSubmitted) {
                    throw new IllegalStateException("Cannot forward to auditors until all administrative contributors have submitted their sections");
                }
            } catch (Exception e) {
                if (e instanceof IllegalStateException) throw (IllegalStateException) e;
                throw new IllegalArgumentException("Failed to validate administrative contributor progress", e);
            }
        }

        List<User> selectedAuditors = selectedAuditorIds.stream()
                .distinct()
                .map(id -> userRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Selected auditor not found: " + id)))
                .toList();

        java.util.Set<String> submissionPosts = resolveSubmissionPosts(submission, forwardedAdministrativePosts);

        for (User auditor : selectedAuditors) {
            validateSelectedAuditor(auditor, auditType, requestedType, submission, submissionPosts);
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            submission.setForwardedToAuditorIds(mapper.writeValueAsString(selectedAuditors.stream().map(User::getId).toList()));
            submission.setForwardedToAuditorNames(mapper.writeValueAsString(selectedAuditors.stream().map(User::getName).toList()));
            submission.setForwardedToAuditorEmails(mapper.writeValueAsString(selectedAuditors.stream().map(User::getEmail).toList()));
            if (forwardedAdministrativePosts != null) {
                submission.setForwardedAdministrativePosts(mapper.writeValueAsString(forwardedAdministrativePosts));
            }
            if (forwardedToAuditorPosts != null) {
                submission.setForwardedToAuditorPosts(mapper.writeValueAsString(forwardedToAuditorPosts));
            }
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
        System.out.println("[AUDIT_DEBUG] assignSelectedAuditorsForReview starting: selectedAuditors=" + selectedAuditors.stream().map(User::getEmail).toList() + ", submissionPosts=" + submissionPosts);
        for (User auditor : selectedAuditors) {
            if ("administrative".equalsIgnoreCase(auditType)) {
                java.util.Set<String> auditorPosts = resolveAdministrativePosts(auditor);
                java.util.Set<String> activePostsForAuditor = new java.util.HashSet<>(auditorPosts);
                activePostsForAuditor.retainAll(submissionPosts);
                
                System.out.println("[AUDIT_DEBUG] Auditor: email=" + auditor.getEmail() + ", auditorPosts=" + auditorPosts + ", activePostsForAuditor=" + activePostsForAuditor);
                
                if (activePostsForAuditor.isEmpty()) {
                    activePostsForAuditor = auditorPosts;
                    System.out.println("[AUDIT_DEBUG] activePostsForAuditor is empty, falling back to all auditorPosts: " + activePostsForAuditor);
                }
                
                for (String post : activePostsForAuditor) {
                    SubmissionAuditorAssignment assignment = SubmissionAuditorAssignment.builder()
                            .submissionId(submission.getId())
                            .auditorId(auditor.getId())
                            .auditorName(auditor.getName())
                            .auditorEmail(auditor.getEmail())
                            .auditorType(requestedType)
                            .category(auditType)
                            .assignedAt(assignedAt)
                            .post(post)
                            .status("PENDING")
                            .build();
                    auditorAssignmentRepository.save(assignment);
                    System.out.println("[AUDIT_DEBUG] Saved assignment: " + assignment.getId() + " for auditor=" + auditor.getEmail() + " post=" + post);
                }
            } else {
                SubmissionAuditorAssignment assignment = SubmissionAuditorAssignment.builder()
                        .submissionId(submission.getId())
                        .auditorId(auditor.getId())
                        .auditorName(auditor.getName())
                        .auditorEmail(auditor.getEmail())
                        .auditorType(requestedType)
                        .category(auditType)
                        .assignedAt(assignedAt)
                        .post(null)
                        .status("PENDING")
                        .build();
                auditorAssignmentRepository.save(assignment);
                System.out.println("[AUDIT_DEBUG] Saved academic assignment: " + assignment.getId() + " for auditor=" + auditor.getEmail());
            }
        }
    }

    private void validateSelectedAuditor(User auditor, String auditType, String requestedType, Submission submission, java.util.Set<String> submissionPosts) {
        String role = normalize(auditor.getRole());
        String accountType = normalize(auditor.getAccountType());
        if (!"auditor".equals(accountType) && (role == null || !role.contains("auditor"))) {
            throw new IllegalArgumentException("Selected user is not an auditor: " + auditor.getEmail());
        }
        
        String auditorCategory = auditor.getCategory() != null && !auditor.getCategory().isBlank() 
                ? auditor.getCategory() 
                : "academic";
        if (!auditorCategory.equalsIgnoreCase(auditType)) {
            throw new IllegalArgumentException("Selected auditor category does not match submission audit type: " + auditor.getEmail());
        }

        String auditorType = auditor.getAuditorType() != null && !auditor.getAuditorType().isBlank()
                ? auditor.getAuditorType()
                : "internal";
        if (!auditorType.equalsIgnoreCase(requestedType)) {
            throw new IllegalArgumentException("Selected auditor type does not match requested auditor type: " + auditor.getEmail());
        }

        if ("academic".equalsIgnoreCase(auditType)) {
            String submissionSchool = SchoolUtils.canonicalizeSchool(submission.getSchool());
            String auditorSchool = SchoolUtils.canonicalizeSchool(auditor.getSchool());
            if (submissionSchool == null || auditorSchool == null || !submissionSchool.equalsIgnoreCase(auditorSchool)) {
                throw new IllegalArgumentException("Academic auditor must match the submission school: " + auditor.getEmail());
            }
        } else if ("administrative".equalsIgnoreCase(auditType)) {
            java.util.Set<String> auditorPosts = resolveAdministrativePosts(auditor);
            if (submissionPosts.isEmpty() || !hasPostOverlap(auditorPosts, submissionPosts)) {
                System.out.println("[AUDIT_DEBUG] Warning: Selected administrative auditor has no overlap with submitted posts: " + auditor.getEmail());
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
                    .allMatch(requiredPost -> {
                        if (!isPostRequiredForAdministrativeWorkflow(requiredPost)) {
                            return true;
                        }
                        String status = progress.path(requiredPost).asText("DRAFT");
                        return "SUBMITTED".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status);
                    });

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

    private void mergePartESchools(ObjectMapper mapper, com.fasterxml.jackson.databind.node.ObjectNode merged, com.fasterxml.jackson.databind.JsonNode incomingPartESchools) {
        com.fasterxml.jackson.databind.JsonNode existingNode = merged.get("partESchools");
        if (existingNode == null || !existingNode.isArray()) {
            merged.set("partESchools", incomingPartESchools);
            return;
        }
        if (incomingPartESchools == null || !incomingPartESchools.isArray()) {
            return;
        }

        com.fasterxml.jackson.databind.node.ArrayNode existingArray = (com.fasterxml.jackson.databind.node.ArrayNode) existingNode;
        com.fasterxml.jackson.databind.node.ArrayNode incomingArray = (com.fasterxml.jackson.databind.node.ArrayNode) incomingPartESchools;

        // Map existing schools by schoolCode
        java.util.Map<String, com.fasterxml.jackson.databind.node.ObjectNode> existingMap = new java.util.LinkedHashMap<>();
        for (com.fasterxml.jackson.databind.JsonNode schoolNode : existingArray) {
            if (schoolNode.isObject()) {
                String code = schoolNode.path("schoolCode").asText("").trim().toUpperCase();
                if (!code.isEmpty()) {
                    existingMap.put(code, (com.fasterxml.jackson.databind.node.ObjectNode) schoolNode);
                }
            }
        }

        com.fasterxml.jackson.databind.node.ArrayNode newArray = mapper.createArrayNode();

        // Process incoming schools
        for (com.fasterxml.jackson.databind.JsonNode incomingSchoolNode : incomingArray) {
            if (incomingSchoolNode.isObject()) {
                com.fasterxml.jackson.databind.node.ObjectNode incomingSchool = (com.fasterxml.jackson.databind.node.ObjectNode) incomingSchoolNode;
                String code = incomingSchool.path("schoolCode").asText("").trim().toUpperCase();
                if (!code.isEmpty() && existingMap.containsKey(code)) {
                    // Merge incoming school keys into existing school to preserve older fields
                    com.fasterxml.jackson.databind.node.ObjectNode existingSchool = existingMap.remove(code);
                    incomingSchool.fields().forEachRemaining(field -> {
                        existingSchool.set(field.getKey(), field.getValue());
                    });
                    newArray.add(existingSchool);
                } else {
                    newArray.add(incomingSchool);
                }
            }
        }

        // Add remaining existing schools that were not in incoming
        existingMap.values().forEach(newArray::add);

        merged.set("partESchools", newArray);
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
            if ("__administrativeSubmissionStatus".equals(entry.getKey())) {
                com.fasterxml.jackson.databind.node.ObjectNode existingStatus = null;
                com.fasterxml.jackson.databind.JsonNode existingNode = merged.get("__administrativeSubmissionStatus");
                if (existingNode != null && existingNode.isObject()) {
                    existingStatus = (com.fasterxml.jackson.databind.node.ObjectNode) existingNode;
                } else {
                    existingStatus = mapper.createObjectNode();
                }
                com.fasterxml.jackson.databind.JsonNode incomingValue = entry.getValue();
                if (incomingValue.isObject()) {
                    existingStatus.setAll((com.fasterxml.jackson.databind.node.ObjectNode) incomingValue);
                }
                merged.set("__administrativeSubmissionStatus", existingStatus);
                return;
            }
            String section = sectionClassifier.apply(entry.getKey());
            if (ownedSections.contains(section)) {
                if ("partESchools".equals(entry.getKey())) {
                    mergePartESchools(mapper, merged, entry.getValue());
                } else {
                    merged.set(entry.getKey(), entry.getValue());
                }
                return;
            }
            if ("partESchools".equals(entry.getKey()) || "coursesOffered".equals(entry.getKey()) || "studentStatistics".equals(entry.getKey())
                    || "facultyInformation".equals(entry.getKey()) || "facultyTenure".equals(entry.getKey())
                    || "facultyExperience".equals(entry.getKey()) || "supportingStaff".equals(entry.getKey())
                    || "staffTraining".equals(entry.getKey()) || "bogMomSanctionedPostsAttachment".equals(entry.getKey())) {
                com.fasterxml.jackson.databind.JsonNode existingValue = merged.get(entry.getKey());
                if (existingValue != null && existingValue.equals(entry.getValue())) {
                    return;
                }
                if (isEffectivelyEmpty(existingValue) && isEffectivelyEmpty(entry.getValue())) {
                    return;
                }
                throw new SecurityException("Unauthorized " + payloadName + " modification for section " + section
                        + " (key: '" + entry.getKey() + "', existing: " + existingValue + ", incoming: " + entry.getValue() + ")");
            }
            com.fasterxml.jackson.databind.JsonNode existingValue = merged.get(entry.getKey());
            if (existingValue != null && existingValue.equals(entry.getValue())) {
                return;
            }
            if (isEffectivelyEmpty(existingValue) && isEffectivelyEmpty(entry.getValue())) {
                return;
            }
            throw new SecurityException("Unauthorized " + payloadName + " modification for section " + section
                    + " (key: '" + entry.getKey() + "', existing: " + existingValue + ", incoming: " + entry.getValue() + ")");
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

    private boolean isSubmittedByActiveUser(com.fasterxml.jackson.databind.node.ObjectNode values, String post) {
        try {
            com.fasterxml.jackson.databind.JsonNode statusNode = values.get("__administrativeSubmissionStatus");
            if (statusNode != null && statusNode.isObject() && statusNode.has(post)) {
                com.fasterxml.jackson.databind.JsonNode postNode = statusNode.get(post);
                if (postNode != null && postNode.path("submitted").asBoolean()) {
                    String email = postNode.path("email").asText(null);
                    if (email != null && !email.isBlank()) {
                        return userRepository.findByEmail(email.trim().toLowerCase())
                                .filter(u -> !Boolean.TRUE.equals(u.getDeleted()))
                                .isPresent();
                    }
                }
            }
        } catch (Exception ignored) {}

        try {
            com.fasterxml.jackson.databind.JsonNode progress = values.get("administrativeProgress");
            if (progress != null && "SUBMITTED".equalsIgnoreCase(progress.path(post).asText())) {
                return hasActiveAdministrativeUserForPost(post);
            }
        } catch (Exception ignored) {}

        return false;
    }

    private void removeAdministrativeSubmissionStatus(com.fasterxml.jackson.databind.node.ObjectNode values, java.util.Set<String> posts) {
        if (values == null) return;
        com.fasterxml.jackson.databind.JsonNode statusNode = values.get("__administrativeSubmissionStatus");
        if (statusNode == null || !statusNode.isObject()) {
            return;
        }
        com.fasterxml.jackson.databind.node.ObjectNode statusObj = (com.fasterxml.jackson.databind.node.ObjectNode) statusNode;
        posts.forEach(statusObj::remove);
    }

    private com.fasterxml.jackson.databind.node.ObjectNode administrativeProgressNode(ObjectMapper mapper,
                                                                                      com.fasterxml.jackson.databind.node.ObjectNode values) {
        com.fasterxml.jackson.databind.JsonNode existing = values.get("administrativeProgress");
        com.fasterxml.jackson.databind.node.ObjectNode progress = existing != null && existing.isObject()
                ? (com.fasterxml.jackson.databind.node.ObjectNode) existing.deepCopy()
                : mapper.createObjectNode();
        ADMIN_POSTS.forEach(post -> {
            if (!hasActiveAdministrativeUserForPost(post)) {
                progress.put(post, "APPROVED");
            } else {
                boolean submittedByActive = isSubmittedByActiveUser(values, post);
                if (!submittedByActive) {
                    progress.put(post, "DRAFT");
                    removeAdministrativeSubmissionStatus(values, java.util.Set.of(post));
                    removeAdministrativeApprovals(values, java.util.Set.of(post));
                } else {
                    progress.put(post, "SUBMITTED");
                }
            }
        });
        return progress;
    }

    private boolean isAdministrativePostSubmitted(com.fasterxml.jackson.databind.node.ObjectNode values, String post) {
        com.fasterxml.jackson.databind.JsonNode progress = values.get("administrativeProgress");
        return progress != null && "SUBMITTED".equalsIgnoreCase(progress.path(post).asText());
    }

    private java.util.Set<String> ownedAdministrativeSections(String post) {
        if (post == null) {
            return java.util.Collections.emptySet();
        }
        return switch (post) {
            case "registrar" -> java.util.Set.of("A", "C");
            case "hr" -> java.util.Set.of("B");
            case "dean-student-welfare" -> java.util.Set.of("D");
            case "dean-placement" -> java.util.Set.of("E");
            default -> java.util.Collections.emptySet();
        };
    }

    private java.util.Set<String> resolveAdministrativePosts(User user) {
        java.util.Set<String> posts = new java.util.LinkedHashSet<>();
        String primaryPost = canonicalAdministrativePost(user.getPost());
        if (primaryPost != null) {
            posts.add(primaryPost);
        }
        if (user.getId() != null) {
            userAdministrativePostRepository.findByUserId(user.getId()).stream()
                    .map(UserAdministrativePost::getPost)
                    .map(this::canonicalAdministrativePost)
                    .filter(post -> post != null && !post.isBlank())
                    .forEach(posts::add);
        }
        return posts;
    }

    private boolean hasActiveAdministrativeUserForPost(String post) {
        if (post == null || post.isBlank()) return false;
        String canonicalPost = canonicalAdministrativePost(post);
        if (canonicalPost == null) return false;
        
        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            if (u.getDeleted() != null && u.getDeleted()) {
                continue;
            }
            if (!"administrative".equalsIgnoreCase(u.getRole())) {
                continue;
            }
            String up = canonicalAdministrativePost(u.getPost());
            if (canonicalPost.equalsIgnoreCase(up)) {
                return true;
            }
            if (u.getId() != null) {
                List<UserAdministrativePost> ups = userAdministrativePostRepository.findByUserId(u.getId());
                for (UserAdministrativePost userPost : ups) {
                    String uPostCanonical = canonicalAdministrativePost(userPost.getPost());
                    if (canonicalPost.equalsIgnoreCase(uPostCanonical)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isPostRequiredForAdministrativeWorkflow(String post) {
        return hasActiveAdministrativeUserForPost(post);
    }

    private void removeAdministrativeOwnedFields(com.fasterxml.jackson.databind.node.ObjectNode node,
                                                 java.util.function.Function<String, String> sectionClassifier,
                                                 java.util.Set<String> sections) {
        List<String> removeKeys = new java.util.ArrayList<>();
        node.fields().forEachRemaining(entry -> {
            if ("administrativeProgress".equals(entry.getKey()) || "administrativeApprovals".equals(entry.getKey())) {
                return;
            }
            if (sections.contains(sectionClassifier.apply(entry.getKey()))) {
                removeKeys.add(entry.getKey());
            }
        });
        removeKeys.forEach(node::remove);
    }

    private void resetAdministrativeProgress(com.fasterxml.jackson.databind.node.ObjectNode values, java.util.Set<String> posts) {
        ObjectMapper mapper = new ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode progress = administrativeProgressNode(mapper, values);
        posts.forEach(post -> progress.put(post, "DRAFT"));
        values.set("administrativeProgress", progress);
    }

    private void removeAdministrativeApprovals(com.fasterxml.jackson.databind.node.ObjectNode values, java.util.Set<String> posts) {
        com.fasterxml.jackson.databind.JsonNode approvalsNode = values.get("administrativeApprovals");
        if (approvalsNode == null || !approvalsNode.isObject()) {
            return;
        }
        com.fasterxml.jackson.databind.node.ObjectNode approvals = (com.fasterxml.jackson.databind.node.ObjectNode) approvalsNode;
        posts.forEach(approvals::remove);
    }

    private String removeAdministrativeSubmittedByDetails(ObjectMapper mapper, String submittedByDetails,
                                                         java.util.Set<String> posts) throws java.io.IOException {
        com.fasterxml.jackson.databind.node.ObjectNode details = objectNodeOrEmpty(mapper, submittedByDetails);
        for (String post : posts) {
            String key = toCamelCaseRole(post);
            com.fasterxml.jackson.databind.node.ObjectNode emptyInfo = mapper.createObjectNode();
            emptyInfo.put("submitted", false);
            emptyInfo.putNull("submittedAt");
            emptyInfo.putNull("name");
            emptyInfo.putNull("email");
            details.set(key, emptyInfo);
        }
        return mapper.writeValueAsString(details);
    }

    private String removeAdministrativeOwnedAttachments(ObjectMapper mapper, String attachmentsJson,
                                                        java.util.Set<String> sections) {
        if (attachmentsJson == null || attachmentsJson.isBlank()) {
            return attachmentsJson;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(attachmentsJson);
            if (!root.isArray()) {
                return attachmentsJson;
            }
            com.fasterxml.jackson.databind.node.ArrayNode retained = mapper.createArrayNode();
            root.forEach(item -> {
                if (attachmentBelongsToAdministrativeSections(item, sections)) {
                    String url = textField(item, "url", "fileUrl", "downloadUrl");
                    if (url != null) {
                        try {
                            attachmentService.deleteFile(url);
                        } catch (Exception e) {
                            System.err.println("Failed to delete attachment: " + url);
                        }
                    }
                } else {
                    retained.add(item);
                }
            });
            return mapper.writeValueAsString(retained);
        } catch (Exception e) {
            return attachmentsJson;
        }
    }

    private boolean attachmentBelongsToAdministrativeSections(com.fasterxml.jackson.databind.JsonNode node,
                                                             java.util.Set<String> sections) {
        if (node == null || !node.isObject()) {
            return false;
        }
        String tableId = textField(node, "tableId", "tableName", "table");
        if (tableId != null && sections.contains(classifyAdministrativeTableSection(tableId))) {
            return true;
        }
        String sectionId = textField(node, "sectionId", "section", "part");
        return sectionId != null && sections.contains(classifyAdministrativeValueSection(sectionId));
    }

    private String classifyAdministrativeTableSection(String key) {
        if (key == null) return "A";
        String normalized = key.trim();
        String lower = normalized.toLowerCase();
        if ("a".equals(lower)) return "A";
        if ("b".equals(lower)) return "B";
        if ("c".equals(lower)) return "C";
        if ("d".equals(lower)) return "D";
        if ("e".equals(lower)) return "E";
        if ("f".equals(lower)) return "F";

        // Section A
        if (List.of("coursesOffered", "studentStatistics", "statutoryBodies", "auditRecords", "scholarshipSummary", "scholarshipStudents").contains(normalized)) {
            return "A";
        }
        // Section B
        if (List.of("facultyInformation", "facultyTenure", "facultyExperience", "supportingStaff", "staffTraining").contains(normalized)) {
            return "B";
        }
        // Section C
        if (List.of("buildingInfrastructure", "libraryInfrastructure", "eResources", "itInfrastructure", "sportsFacilities", "divyangajanFacilities", "researchResources").contains(normalized)) {
            return "C";
        }
        // Section D
        if (List.of("hackathons", "culturalActivities", "sportsActivities", "communityActivities", "adminStudentAwards").contains(normalized)) {
            return "D";
        }
        // Section E
        if (List.of("trainingActivities", "industryCollaborations").contains(normalized) || normalized.toLowerCase().contains("placement")) {
            return "E";
        }

        // Fallback heuristics
        if (lower.contains("faculty") || lower.contains("staff")) {
            return "B";
        }
        if (lower.contains("statutory") || lower.contains("infrastructure") || lower.contains("library")
                || lower.contains("eresource") || lower.contains("it") || lower.contains("sportsfacilities")
                || lower.contains("divyangajan") || lower.contains("researchresource")) {
            return "C";
        }
        if (lower.contains("hackathon") || lower.contains("cultural") || lower.contains("sports")
                || lower.contains("community") || lower.contains("adminstudentawards") || lower.contains("awardsprizes")) {
            return "D";
        }
        return "A";
    }

    private String classifyAdministrativeValueSection(String key) {
        if (key == null) return "A";
        String normalized = key.trim();
        String lower = normalized.toLowerCase();
        if ("a".equals(lower)) return "A";
        if ("b".equals(lower)) return "B";
        if ("c".equals(lower)) return "C";
        if ("d".equals(lower)) return "D";
        if ("e".equals(lower)) return "E";
        if ("f".equals(lower)) return "F";

        // Section B
        if (List.of("phdQualification", "pgQualification", "otherQualification", "studentFacultyRatio", "bogMomSanctionedPostsAttachment").contains(normalized)) {
            return "B";
        }
        // Section E
        if ("partESchools".equals(normalized)) {
            return "E";
        }
        // Section F
        if (List.of("auditObservations", "auditRecommendations").contains(normalized)) {
            return "F";
        }

        // Fallback heuristics
        if (lower.contains("partb") || lower.contains("hr") || lower.contains("faculty") || lower.contains("staff")
                || lower.contains("bogmom")) {
            return "B";
        }
        if (lower.contains("partc") || lower.contains("infrastructure") || lower.contains("statutory")
                || lower.contains("library") || lower.contains("researchresource")) {
            return "C";
        }
        if (lower.contains("partd") || lower.contains("hackathon") || lower.contains("cultural")
                || lower.contains("sports") || lower.contains("community")
                || lower.contains("adminstudentawards") || lower.contains("awardsprizesrecognitions")) {
            return "D";
        }
        if (lower.contains("parte") || lower.contains("placement") || lower.contains("parteschools")
                || lower.contains("trainingactivities") || lower.contains("industrycollaboration")) {
            return "E";
        }
        if (lower.contains("partf") || lower.contains("observation") || lower.contains("recommendation")) {
            return "F";
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
            case "dean-placement", "placement", "dean-of-placement", "deanplacement" -> "dean-placement";
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
        if (sanitized == null || sanitized.isBlank()) {
            throw new IllegalArgumentException("Attachment filename is invalid");
        }

        String size = textField(node, "size", "fileSize");
        if (size != null) {
            try {
                long parsedSize = Long.parseLong(size);
                if (parsedSize > AttachmentService.MAX_FILE_SIZE_BYTES) {
                    throw new IllegalArgumentException("Attachment file size exceeds maximum limit of 25MB");
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
        populateAuditorProgressAndAssignments(submission);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", submission.getId());
        response.put("submissionId", submission.getId());
        response.put("version", submission.getVersion());
        response.put("academicYear", submission.getAcademicYear() != null ? submission.getAcademicYear() : submission.getAuditCycle());
        response.put("auditCycle", submission.getAuditCycle());
        response.put("schoolGroup", submission.getSchoolGroup());
        response.put("reportCategory", submission.getReportCategory());
        response.put("approvedReportCategory", submission.getReportCategory());
        response.put("status", submission.getStatus());
        response.put("overallStatus", submission.getStatus());
        response.put("valuesData", submission.getValuesData());
        response.put("values", submission.getValuesData());
        response.put("tablesData", submission.getTablesData());
        response.put("tables", submission.getTablesData());
        response.put("attachments", submission.getAttachments());
        response.put("remarks", submission.getRemarks());
        response.put("auditorReviewedBy", submission.getAuditorReviewedBy());
        response.put("auditorReviewedOn", submission.getAuditorReviewedOn());
        response.put("approvedByName", submission.getApprovedByName());
        response.put("approvedAt", submission.getApprovedAt());
        response.put("auditorAssignments", submission.getAuditorAssignments());
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

    private String resolvePostForKey(String key) {
        if (key == null) return "registrar";
        String lower = key.toLowerCase();
        
        // Explicit post name matches (priority)
        if (lower.contains("library")) return "library";
        if (lower.contains("examination") || lower.contains("exam")) return "examination";
        if (lower.contains("accounts") || lower.contains("account") || lower.contains("finance")) return "accounts";
        if (lower.contains("hr") || lower.contains("human-resource") || lower.contains("faculty") || lower.contains("staff")) return "hr";
        if (lower.contains("placement") || lower.contains("training") || lower.contains("progression")) return "dean-placement";
        if (lower.contains("student-welfare") || lower.contains("dsw") || lower.contains("sports") || lower.contains("cultural") || lower.contains("hackathon") || lower.contains("community")) return "dean-student-welfare";
        if (lower.contains("registrar")) return "registrar";
        
        // Section heuristics (falling back to the section owners)
        String section = classifyAdministrativeValueSection(key);
        if ("A".equals(section) || "C".equals(section)) {
            return "registrar";
        } else if ("B".equals(section)) {
            return "hr";
        } else if ("D".equals(section)) {
            return "dean-student-welfare";
        } else if ("E".equals(section)) {
            return "dean-placement";
        }
        
        return "registrar"; // default fallback
    }

    public void validateAuditorAccessToUpdates(Submission submission, User auditor, String incomingValuesData, String incomingTablesData) {
        ObjectMapper mapper = new ObjectMapper();
        java.util.Set<String> assignedPosts = resolveAdministrativePosts(auditor);
        
        // Map assigned posts to canonical form for matching
        java.util.Set<String> canonicalAssignedPosts = new java.util.HashSet<>();
        for (String post : assignedPosts) {
            String cp = canonicalAdministrativePost(post);
            if (cp != null) {
                canonicalAssignedPosts.add(cp);
            }
        }

        try {
            // 1. Check valuesData changes
            if (incomingValuesData != null) {
                com.fasterxml.jackson.databind.node.ObjectNode existingValues = objectNodeOrEmpty(mapper, submission.getValuesData());
                com.fasterxml.jackson.databind.node.ObjectNode incomingValues = objectNodeOrEmpty(mapper, incomingValuesData);
                
                // Collect all keys in both existing and incoming
                java.util.Set<String> allValueKeys = new java.util.HashSet<>();
                existingValues.fieldNames().forEachRemaining(allValueKeys::add);
                incomingValues.fieldNames().forEachRemaining(allValueKeys::add);
                
                for (String key : allValueKeys) {
                    if (List.of("auditorSignOff", "administrativeProgress", "administrativeApprovals").contains(key)) {
                        continue;
                    }
                    
                    com.fasterxml.jackson.databind.JsonNode val1 = existingValues.get(key);
                    com.fasterxml.jackson.databind.JsonNode val2 = incomingValues.get(key);
                    
                    if (val1 == null || val2 == null || !val1.equals(val2)) {
                        String keyPost = resolvePostForKey(key);
                        String canonicalKeyPost = canonicalAdministrativePost(keyPost);
                        
                        if (!canonicalAssignedPosts.contains(canonicalKeyPost)) {
                            log.warn("Auditor {} (assigned: {}) attempted unauthorized update to field '{}' belonging to post '{}'",
                                    auditor.getEmail(), canonicalAssignedPosts, key, canonicalKeyPost);
                            throw new SecurityException("You do not have permission to edit fields for the post: " + keyPost);
                        }
                    }
                }
            }

            // 2. Check tablesData changes
            if (incomingTablesData != null) {
                com.fasterxml.jackson.databind.node.ObjectNode existingTables = objectNodeOrEmpty(mapper, submission.getTablesData());
                com.fasterxml.jackson.databind.node.ObjectNode incomingTables = objectNodeOrEmpty(mapper, incomingTablesData);
                
                java.util.Set<String> allTableKeys = new java.util.HashSet<>();
                existingTables.fieldNames().forEachRemaining(allTableKeys::add);
                incomingTables.fieldNames().forEachRemaining(allTableKeys::add);
                
                for (String key : allTableKeys) {
                    com.fasterxml.jackson.databind.JsonNode val1 = existingTables.get(key);
                    com.fasterxml.jackson.databind.JsonNode val2 = incomingTables.get(key);
                    
                    if (val1 == null || val2 == null || !val1.equals(val2)) {
                        String keyPost = resolvePostForKey(key);
                        String canonicalKeyPost = canonicalAdministrativePost(keyPost);
                        
                        if (!canonicalAssignedPosts.contains(canonicalKeyPost)) {
                            log.warn("Auditor {} (assigned: {}) attempted unauthorized update to table '{}' belonging to post '{}'",
                                    auditor.getEmail(), canonicalAssignedPosts, key, canonicalKeyPost);
                            throw new SecurityException("You do not have permission to edit tables for the post: " + keyPost);
                        }
                    }
                }
            }
        } catch (SecurityException se) {
            throw se;
        } catch (Exception e) {
            log.error("Error validating auditor access: {}", e.getMessage(), e);
        }
    }

    public void populatePermissions(Submission submission, User user) {
        if (submission == null) return;
        
        populateAuditorProgressAndAssignments(submission);
        populateNextCycleLinkage(submission);
        
        boolean isAuditor = user.getRole().toLowerCase().contains("auditor") || "auditor".equalsIgnoreCase(user.getAccountType());
        boolean isIqac = "iqac".equalsIgnoreCase(user.getRole());
        boolean isVc = "vice-chancellor".equalsIgnoreCase(user.getRole());
        
        java.util.Map<String, Object> permissionMap = new java.util.HashMap<>();
        
        // Determine cycleType and version
        String cycleType = submission.getReportCategory() != null ? submission.getReportCategory().toLowerCase() : "internal";
        int version = submission.getVersion() != null ? submission.getVersion() : 1;
        
        // Calculate contribution details for administrative user
        boolean canEditContribution = false;
        String contributionStatus = "pending";
        
        if ("administrative".equalsIgnoreCase(submission.getAuditType())) {
            String role = user.getRole() != null ? user.getRole().toLowerCase() : "";
            if ("administrative".equals(role)) {
                String post = canonicalAdministrativePost(user.getPost());
                if (post != null) {
                    String overallStatus = submission.getStatus();
                    boolean isDraft = "DRAFT".equalsIgnoreCase(overallStatus) || "SENT_BACK".equalsIgnoreCase(overallStatus);
                    if (isDraft) {
                        java.util.Map<String, String> progress = submission.getAdministrativeProgressForJson();
                        String postStatus = progress.get(post);
                        if (postStatus == null || "DRAFT".equalsIgnoreCase(postStatus) || "IN_PROGRESS".equalsIgnoreCase(postStatus)) {
                            canEditContribution = true;
                            contributionStatus = "pending";
                        } else {
                            contributionStatus = postStatus.toLowerCase();
                        }
                    } else {
                        contributionStatus = overallStatus.toLowerCase();
                    }
                }
            }
        }
        
        boolean canForwardToAuditor = false;
        if (isIqac || isVc) {
            String overallStatus = submission.getStatus();
            if (List.of("SUBMITTED", "UNDER_REVIEW", "FORWARDED_TO_INTERNAL_AUDITOR", "FORWARDED_TO_EXTERNAL_AUDITOR").contains(overallStatus != null ? overallStatus.toUpperCase() : "")) {
                canForwardToAuditor = true;
            }
        }
        
        boolean allContributorsSubmitted = true;
        if ("administrative".equalsIgnoreCase(submission.getAuditType())) {
            java.util.Map<String, String> progress = submission.getAdministrativeProgressForJson();
            allContributorsSubmitted = ADMIN_POSTS.stream()
                    .allMatch(post -> {
                        if (!isPostRequiredForAdministrativeWorkflow(post)) {
                            return true;
                        }
                        String st = progress.get(post);
                        return "APPROVED".equalsIgnoreCase(st) || "SUBMITTED".equalsIgnoreCase(st);
                    });
        }
        
        permissionMap.put("canEditContribution", canEditContribution);
        permissionMap.put("cycleType", cycleType);
        permissionMap.put("version", version);
        permissionMap.put("contributionStatus", contributionStatus);
        permissionMap.put("canForwardToAuditor", canForwardToAuditor);
        permissionMap.put("allContributorsSubmitted", allContributorsSubmitted);

        if ("administrative".equalsIgnoreCase(submission.getAuditType())) {
            if (isIqac || isVc) {
                permissionMap.put("canView", true);
                permissionMap.put("editablePosts", java.util.Collections.emptyList());
                permissionMap.put("readOnlyPosts", java.util.List.of("registrar", "hr", "dean-student-welfare", "dean-placement", "library", "examination", "accounts"));
                
                java.util.Map<String, Object> perPostPerms = new java.util.HashMap<>();
                for (String p : java.util.List.of("registrar", "hr", "dean-student-welfare", "dean-placement", "library", "examination", "accounts")) {
                    perPostPerms.put(p, java.util.Map.of("canEdit", false));
                }
                permissionMap.put("permissions", perPostPerms);
            } else if (isAuditor) {
                java.util.Set<String> assignedPosts = resolveAdministrativePosts(user);
                
                java.util.List<String> editablePosts = assignedPosts.stream()
                        .map(p -> p.trim().toLowerCase())
                        .collect(java.util.stream.Collectors.toList());
                        
                java.util.List<String> allPosts = java.util.List.of("registrar", "hr", "dean-student-welfare", "dean-placement", "library", "examination", "accounts");
                
                java.util.List<String> readOnlyPosts = new java.util.ArrayList<>();
                java.util.Map<String, Object> perPostPerms = new java.util.HashMap<>();
                
                for (String post : allPosts) {
                    boolean canEdit = editablePosts.contains(post);
                    if (!canEdit) {
                        readOnlyPosts.add(post);
                    }
                    perPostPerms.put(post, java.util.Map.of("canEdit", canEdit));
                }
                
                permissionMap.put("canView", true);
                permissionMap.put("editablePosts", editablePosts);
                permissionMap.put("readOnlyPosts", readOnlyPosts);
                permissionMap.put("permissions", perPostPerms);
            } else {
                permissionMap.put("canView", true);
                permissionMap.put("editablePosts", java.util.Collections.emptyList());
                permissionMap.put("readOnlyPosts", java.util.Collections.emptyList());
                permissionMap.put("permissions", java.util.Collections.emptyMap());
            }
        } else {
            permissionMap.put("canView", true);
            permissionMap.put("editablePosts", java.util.Collections.emptyList());
            permissionMap.put("readOnlyPosts", java.util.Collections.emptyList());
            permissionMap.put("permissions", java.util.Collections.emptyMap());
        }
        
        submission.setPermissions(permissionMap);
    }

    private java.util.Set<String> resolveSubmissionPosts(Submission submission, List<String> requestForwardedPosts) {
        java.util.Set<String> posts = new java.util.HashSet<>();
        
        if (requestForwardedPosts != null) {
            requestForwardedPosts.stream()
                .map(this::canonicalAdministrativePost)
                .filter(p -> p != null && !p.isBlank())
                .forEach(posts::add);
        }
        
        try {
            java.util.Map<String, String> progress = submission.getAdministrativeProgressForJson();
            if (progress != null) {
                progress.forEach((post, status) -> {
                    if (status != null && List.of("SUBMITTED", "APPROVED", "UNDER_REVIEW", "AUDITOR_COMPLETED").contains(status.toUpperCase())) {
                        String np = canonicalAdministrativePost(post);
                        if (np != null && !np.isBlank()) {
                            posts.add(np);
                        }
                    }
                });
            }
        } catch (Exception ignored) {}
        
        try {
            if (submission.getValuesData() != null && !submission.getValuesData().isBlank()) {
                ObjectMapper mapper = new ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(submission.getValuesData());
                com.fasterxml.jackson.databind.JsonNode statusNode = rootNode.get("__administrativeSubmissionStatus");
                if (statusNode != null && statusNode.isObject()) {
                    statusNode.fields().forEachRemaining(entry -> {
                        com.fasterxml.jackson.databind.JsonNode subNode = entry.getValue().get("submitted");
                        if (subNode != null && subNode.asBoolean()) {
                            String np = canonicalAdministrativePost(entry.getKey());
                            if (np != null && !np.isBlank()) {
                                posts.add(np);
                            }
                        }
                    });
                }
            }
        } catch (Exception ignored) {}

        return posts;
    }

    private java.util.Set<String> resolveSubmissionPostsForList(Submission submission) {
        java.util.Set<String> posts = new java.util.HashSet<>();
        
        String favPosts = submission.getForwardedAdministrativePosts();
        if (favPosts != null && !favPosts.isBlank()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                java.util.List<String> list = mapper.readValue(favPosts, java.util.List.class);
                if (list != null) {
                    list.stream()
                        .map(this::canonicalAdministrativePost)
                        .filter(p -> p != null && !p.isBlank())
                        .forEach(posts::add);
                }
            } catch (Exception ignored) {}
        }
        
        try {
            java.util.Map<String, String> progress = submission.getAdministrativeProgressForJson();
            if (progress != null) {
                progress.forEach((post, status) -> {
                    if (status != null && List.of("SUBMITTED", "APPROVED", "UNDER_REVIEW", "AUDITOR_COMPLETED").contains(status.toUpperCase())) {
                        String np = canonicalAdministrativePost(post);
                        if (np != null && !np.isBlank()) {
                            posts.add(np);
                        }
                    }
                });
            }
        } catch (Exception ignored) {}
        
        try {
            if (submission.getValuesData() != null && !submission.getValuesData().isBlank()) {
                ObjectMapper mapper = new ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(submission.getValuesData());
                com.fasterxml.jackson.databind.JsonNode statusNode = rootNode.get("__administrativeSubmissionStatus");
                if (statusNode != null && statusNode.isObject()) {
                    statusNode.fields().forEachRemaining(entry -> {
                        com.fasterxml.jackson.databind.JsonNode subNode = entry.getValue().get("submitted");
                        if (subNode != null && subNode.asBoolean()) {
                            String np = canonicalAdministrativePost(entry.getKey());
                            if (np != null && !np.isBlank()) {
                                posts.add(np);
                            }
                        }
                    });
                }
            }
        } catch (Exception ignored) {}

        return posts;
    }

    private boolean hasPostOverlap(java.util.Set<String> auditorPosts, java.util.Set<String> submissionPosts) {
        java.util.Set<String> normalizedAuditor = new java.util.HashSet<>();
        for (String p : auditorPosts) {
            String cp = canonicalAdministrativePost(p);
            if (cp != null) normalizedAuditor.add(cp);
        }
        
        java.util.Set<String> normalizedSubmission = new java.util.HashSet<>();
        for (String p : submissionPosts) {
            String cp = canonicalAdministrativePost(p);
            if (cp != null) normalizedSubmission.add(cp);
        }
        
        normalizedAuditor.retainAll(normalizedSubmission);
        return !normalizedAuditor.isEmpty();
    }

    @Transactional
    public java.util.Map<String, Object> submitAuditorReview(
            Long submissionId,
            User caller,
            com.director_appraisal.director_appraisal.controller.SubmissionController.AuditorSubmitRequest request) {
            
        Submission submission = submissionRepository.findByIdForUpdate(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + submissionId));
                
        java.util.Set<String> auditorPosts = resolveAdministrativePosts(caller);
        List<SubmissionAuditorAssignment> allAssignments = auditorAssignmentRepository.findBySubmissionId(submissionId);
        
        System.out.println("[AUDIT_DEBUG] Caller: id=" + caller.getId() + ", email=" + caller.getEmail() + ", name=" + caller.getName());
        System.out.println("[AUDIT_DEBUG] Resolved auditorPosts: " + auditorPosts);
        System.out.println("[AUDIT_DEBUG] Total assignments in DB for submission " + submissionId + ": " + allAssignments.size());
        for (SubmissionAuditorAssignment a : allAssignments) {
            System.out.println("[AUDIT_DEBUG] Assignment: id=" + a.getId() + ", auditorId=" + a.getAuditorId() + ", email=" + a.getAuditorEmail() + ", post=" + a.getPost() + ", status=" + a.getStatus());
        }

        boolean isAssigned = false;
        List<SubmissionAuditorAssignment> assignments = new java.util.ArrayList<>();
        
        if (!allAssignments.isEmpty()) {
            List<SubmissionAuditorAssignment> callerAssignments = allAssignments.stream()
                    .filter(a -> caller.getId().equals(a.getAuditorId()) || (caller.getEmail() != null && caller.getEmail().equalsIgnoreCase(a.getAuditorEmail())))
                    .toList();
                    
            System.out.println("[AUDIT_DEBUG] callerAssignments matched count: " + callerAssignments.size());
            if (!callerAssignments.isEmpty()) {
                for (SubmissionAuditorAssignment assignment : callerAssignments) {
                    String canonicalPost = canonicalAdministrativePost(assignment.getPost());
                    boolean hasOverlap = canonicalPost != null && auditorPosts.contains(canonicalPost);
                    System.out.println("[AUDIT_DEBUG] Checking assignment: id=" + assignment.getId() + ", post=" + assignment.getPost() + ", canonicalPost=" + canonicalPost + ", hasOverlap=" + hasOverlap);
                    if ("administrative".equalsIgnoreCase(submission.getAuditType())) {
                        if (canonicalPost != null && auditorPosts.contains(canonicalPost)) {
                            isAssigned = true;
                            assignments.add(assignment);
                        }
                    } else {
                        isAssigned = true;
                        assignments.add(assignment);
                    }
                }
            }
        } else {
            // Check legacy fields
            boolean emailOrIdMatch = false;
            if (submission.getForwardedToAuditorId() != null && submission.getForwardedToAuditorId().equals(caller.getId())) {
                emailOrIdMatch = true;
            } else if (caller.getEmail() != null && caller.getEmail().equalsIgnoreCase(submission.getForwardedToAuditorEmail())) {
                emailOrIdMatch = true;
            } else {
                String idsStr = submission.getForwardedToAuditorIds();
                if (idsStr != null && !idsStr.isBlank()) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        java.util.List<?> list = mapper.readValue(idsStr, java.util.List.class);
                        if (list != null) {
                            for (Object obj : list) {
                                if (obj != null && obj.toString().equals(caller.getId().toString())) {
                                    emailOrIdMatch = true;
                                    break;
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
                if (!emailOrIdMatch) {
                    String emailsStr = submission.getForwardedToAuditorEmails();
                    if (emailsStr != null && !emailsStr.isBlank() && caller.getEmail() != null) {
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            java.util.List<?> list = mapper.readValue(emailsStr, java.util.List.class);
                            if (list != null) {
                                for (Object obj : list) {
                                    if (obj != null && caller.getEmail().equalsIgnoreCase(obj.toString().trim())) {
                                        emailOrIdMatch = true;
                                        break;
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
            if (emailOrIdMatch) {
                if ("administrative".equalsIgnoreCase(submission.getAuditType())) {
                    java.util.Set<String> subPosts = resolveSubmissionPostsForList(submission);
                    java.util.Set<String> overlap = new java.util.HashSet<>(auditorPosts);
                    overlap.retainAll(subPosts);
                    if (!overlap.isEmpty()) {
                        isAssigned = true;
                    }
                } else {
                    isAssigned = true;
                }
            }
        }
        
        if (!isAssigned) {
            throw new SecurityException("You are not assigned to review this submission");
        }
        
        // Consolidate postsSubmitted from all possible payload fields
        java.util.Set<String> postsToSubmit = new java.util.HashSet<>();
        if (request.getPostsSubmitted() != null) {
            for (String p : request.getPostsSubmitted()) {
                String cp = canonicalAdministrativePost(p);
                if (cp != null) postsToSubmit.add(cp);
            }
        }
        if (request.getSubmittedPosts() != null) {
            for (String p : request.getSubmittedPosts()) {
                String cp = canonicalAdministrativePost(p);
                if (cp != null) postsToSubmit.add(cp);
            }
        }
        if (request.getAdministrativePosts() != null) {
            for (String p : request.getAdministrativePosts()) {
                String cp = canonicalAdministrativePost(p);
                if (cp != null) postsToSubmit.add(cp);
            }
        }
        if (request.getAssignedPosts() != null) {
            for (String p : request.getAssignedPosts()) {
                String cp = canonicalAdministrativePost(p);
                if (cp != null) postsToSubmit.add(cp);
            }
        }
        if (request.getPosts() != null) {
            for (String p : request.getPosts()) {
                String cp = canonicalAdministrativePost(p);
                if (cp != null) postsToSubmit.add(cp);
            }
        }
        
        LocalDateTime submittedAt = LocalDateTime.now();
        
        for (SubmissionAuditorAssignment assignment : assignments) {
            boolean shouldUpdate = false;
            if ("administrative".equalsIgnoreCase(submission.getAuditType())) {
                String assPost = canonicalAdministrativePost(assignment.getPost());
                if (postsToSubmit.isEmpty() || assPost == null || postsToSubmit.contains(assPost)) {
                    shouldUpdate = true;
                }
            } else {
                shouldUpdate = true;
            }
            
            if (shouldUpdate) {
                assignment.setStatus("SUBMITTED");
                assignment.setSubmittedAt(submittedAt);
                assignment.setValuesData(request.getValuesData());
                assignment.setTablesData(request.getTablesData());
                assignment.setAttachments(request.getAttachments());
                auditorAssignmentRepository.save(assignment);
            }
        }
        
        if ("administrative".equalsIgnoreCase(submission.getAuditType())) {
            String preparedAttachments = request.getAttachments() != null ? deduplicateAttachmentMetadataJson(request.getAttachments()) : submission.getAttachments();
            AdministrativePayload administrativePayload = prepareAdministrativePayload(submission, caller, request.getValuesData(), request.getTablesData(), preparedAttachments, false);
            if (request.getValuesData() != null) {
                submission.setValuesData(administrativePayload.valuesData());
            }
            if (request.getTablesData() != null) {
                submission.setTablesData(prepareTablesDataUpdate(submission, administrativePayload.tablesData(), preparedAttachments));
            }
            if (request.getAttachments() != null) {
                submission.setAttachments(preparedAttachments);
            }
        } else {
            if (request.getValuesData() != null) {
                submission.setValuesData(injectAuditorSignOff(request.getValuesData(), caller));
            }
            if (request.getTablesData() != null) {
                submission.setTablesData(request.getTablesData());
            }
            if (request.getAttachments() != null) {
                submission.setAttachments(request.getAttachments());
            }
        }
        
        allAssignments = auditorAssignmentRepository.findBySubmissionIdAndAuditorType(submissionId, submission.getForwardedAuditorType());
        if (allAssignments.isEmpty()) {
            allAssignments = auditorAssignmentRepository.findBySubmissionId(submissionId);
        }
        
        java.util.List<SubmissionAuditorAssignment> validAssignments = allAssignments;
        if ("administrative".equalsIgnoreCase(submission.getAuditType())) {
            java.util.Set<String> validAdminPosts = java.util.Set.of("registrar", "hr", "dean-placement", "dean-student-welfare");
            validAssignments = allAssignments.stream()
                    .filter(a -> {
                        String postCanonical = canonicalAdministrativePost(a.getPost());
                        return postCanonical != null && validAdminPosts.contains(postCanonical);
                    })
                    .collect(java.util.stream.Collectors.toList());
        }
        
        int total = validAssignments.size();
        int submitted = (int) validAssignments.stream().filter(a -> "SUBMITTED".equalsIgnoreCase(a.getStatus())).count();
        int pending = total - submitted;
        boolean allSubmitted = (total > 0 && pending == 0);
        
        if (allSubmitted || (total == 0 && !"administrative".equalsIgnoreCase(submission.getAuditType()))) {
            submission.setStatus("AUDITOR_COMPLETED");
            submission.setAuditorReviewedBy(caller.getName());
            submission.setAuditorReviewedByEmail(caller.getEmail());
            submission.setAuditorReviewedByDesignation(caller.getDesignation());
            submission.setAuditorReviewedByRole(caller.getRole());
            submission.setAuditorReviewedOn(submittedAt);
            
            submission.setAuditorCorrectionRequested(false);
            submission.setCorrectionRequestedForAuditor(false);
            submission.setRequiresAuditorResubmission(false);
            submission.setAuditorResubmittedAt(submittedAt);
        } else {
            submission.setStatus("UNDER_REVIEW");
        }
        
        Submission saved = submissionRepository.save(submission);
        persistDataForStatus(saved);
        
        return buildAuditorSubmitResponse(saved, submission.getForwardedAuditorType());
    }

    public java.util.Map<String, Object> buildAuditorSubmitResponse(Submission submission, String auditorType) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        
        java.util.Map<String, Object> subMap = new java.util.HashMap<>();
        subMap.put("id", submission.getId());
        subMap.put("status", submission.getStatus());
        
        List<SubmissionAuditorAssignment> allAssignments = auditorAssignmentRepository.findBySubmissionIdAndAuditorType(submission.getId(), auditorType);
        if (allAssignments.isEmpty()) {
            allAssignments = auditorAssignmentRepository.findBySubmissionId(submission.getId());
        }
        
        java.util.List<SubmissionAuditorAssignment> validAssignments = allAssignments;
        if ("administrative".equalsIgnoreCase(submission.getAuditType())) {
            java.util.Set<String> validAdminPosts = java.util.Set.of("registrar", "hr", "dean-placement", "dean-student-welfare");
            validAssignments = allAssignments.stream()
                    .filter(a -> {
                        String postCanonical = canonicalAdministrativePost(a.getPost());
                        return postCanonical != null && validAdminPosts.contains(postCanonical);
                    })
                    .collect(java.util.stream.Collectors.toList());
        }
        
        ObjectMapper mapper = new ObjectMapper();
        java.util.List<java.util.Map<String, Object>> assignmentsList = new java.util.ArrayList<>();
        int total = validAssignments.size();
        int submitted = 0;
        
        java.util.Map<String, java.util.Map<String, Object>> byPostMap = new java.util.LinkedHashMap<>();
        
        for (SubmissionAuditorAssignment assignment : validAssignments) {
            boolean isSub = "SUBMITTED".equalsIgnoreCase(assignment.getStatus());
            if (isSub) submitted++;
            
            java.util.Map<String, Object> assMap = new java.util.HashMap<>();
            String postKey = assignment.getPost() != null ? assignment.getPost() : "academic";
            assMap.put("key", submission.getId() + "-" + assignment.getAuditorId() + "-" + postKey);
            assMap.put("auditorId", assignment.getAuditorId());
            assMap.put("auditorName", assignment.getAuditorName());
            assMap.put("auditorEmail", assignment.getAuditorEmail());
            assMap.put("auditorType", assignment.getAuditorType());
            assMap.put("auditCategory", assignment.getCategory());
            assMap.put("post", assignment.getPost());
            assMap.put("status", assignment.getStatus().toLowerCase());
            assMap.put("submittedAt", assignment.getSubmittedAt() != null ? assignment.getSubmittedAt().toString() : null);
            
            try {
                assMap.put("values", assignment.getValuesData() != null ? mapper.readTree(assignment.getValuesData()) : mapper.createObjectNode());
            } catch (Exception e) {
                assMap.put("values", mapper.createObjectNode());
            }
            try {
                assMap.put("attachments", assignment.getAttachments() != null ? mapper.readTree(assignment.getAttachments()) : mapper.createArrayNode());
            } catch (Exception e) {
                assMap.put("attachments", mapper.createArrayNode());
            }
            assignmentsList.add(assMap);
            
            if ("administrative".equalsIgnoreCase(submission.getAuditType()) && assignment.getPost() != null) {
                String p = assignment.getPost();
                java.util.Map<String, Object> postStat = byPostMap.get(p);
                if (postStat == null) {
                    postStat = new java.util.HashMap<>();
                    postStat.put("post", p);
                    postStat.put("total", 0);
                    postStat.put("submitted", 0);
                    postStat.put("pending", 0);
                    byPostMap.put(p, postStat);
                }
                postStat.put("total", (int) postStat.get("total") + 1);
                if (isSub) {
                    postStat.put("submitted", (int) postStat.get("submitted") + 1);
                } else {
                    postStat.put("pending", (int) postStat.get("pending") + 1);
                }
            }
        }
        
        int pending = total - submitted;
        boolean allSubmitted = (total > 0 && pending == 0);
        
        subMap.put("allAuditorsSubmitted", allSubmitted);
        subMap.put("auditorAssignments", assignmentsList);
        
        java.util.Map<String, Object> progress = new java.util.HashMap<>();
        progress.put("total", total);
        progress.put("submitted", submitted);
        progress.put("pending", pending);
        progress.put("allSubmitted", allSubmitted);
        progress.put("byPost", new java.util.ArrayList<>(byPostMap.values()));
        
        subMap.put("auditorProgress", progress);
        response.put("submission", subMap);
        
        return response;
    }

    public void populateAuditorProgressAndAssignments(Submission submission) {
        if (submission == null || submission.getId() == null) return;
        
        List<SubmissionAuditorAssignment> currentAssignments = auditorAssignmentRepository.findBySubmissionId(submission.getId());
        List<SubmissionAuditorAssignment> allAssignments = new java.util.ArrayList<>(currentAssignments);
        
        java.util.Set<Long> loadedSubmissionIds = new java.util.HashSet<>();
        loadedSubmissionIds.add(submission.getId());
        
        Long currentParentId = submission.getParentSubmissionId();
        while (currentParentId != null && !loadedSubmissionIds.contains(currentParentId)) {
            loadedSubmissionIds.add(currentParentId);
            allAssignments.addAll(auditorAssignmentRepository.findBySubmissionId(currentParentId));
            Optional<Submission> parentSub = submissionRepository.findById(currentParentId);
            currentParentId = parentSub.map(Submission::getParentSubmissionId).orElse(null);
        }
        
        if (submission.getRootSubmissionId() != null && !loadedSubmissionIds.contains(submission.getRootSubmissionId())) {
            allAssignments.addAll(auditorAssignmentRepository.findBySubmissionId(submission.getRootSubmissionId()));
        }
        
        java.util.List<SubmissionAuditorAssignment> currentValidAssignments = currentAssignments;
        java.util.List<SubmissionAuditorAssignment> allValidAssignments = allAssignments;
        
        if ("administrative".equalsIgnoreCase(submission.getAuditType())) {
            java.util.Set<String> validAdminPosts = java.util.Set.of("registrar", "hr", "dean-placement", "dean-student-welfare");
            currentValidAssignments = currentAssignments.stream()
                    .filter(a -> {
                        String postCanonical = canonicalAdministrativePost(a.getPost());
                        return postCanonical != null && validAdminPosts.contains(postCanonical);
                    })
                    .collect(java.util.stream.Collectors.toList());
                    
            allValidAssignments = allAssignments.stream()
                    .filter(a -> {
                        String postCanonical = canonicalAdministrativePost(a.getPost());
                        return postCanonical != null && validAdminPosts.contains(postCanonical);
                    })
                    .collect(java.util.stream.Collectors.toList());
        }
        
        ObjectMapper mapper = new ObjectMapper();
        java.util.List<java.util.Map<String, Object>> assignmentsList = new java.util.ArrayList<>();
        int total = currentValidAssignments.size();
        int submitted = 0;
        
        java.util.Map<String, java.util.Map<String, Object>> byPostMap = new java.util.LinkedHashMap<>();
        
        for (SubmissionAuditorAssignment assignment : allValidAssignments) {
            boolean isSub = "SUBMITTED".equalsIgnoreCase(assignment.getStatus());
            if (assignment.getSubmissionId().equals(submission.getId()) && isSub) {
                submitted++;
            }
            
            java.util.Map<String, Object> assMap = new java.util.HashMap<>();
            String postKey = assignment.getPost() != null ? assignment.getPost() : "academic";
            assMap.put("key", assignment.getSubmissionId() + "-" + assignment.getAuditorId() + "-" + postKey);
            assMap.put("auditorId", assignment.getAuditorId());
            assMap.put("auditorName", assignment.getAuditorName());
            assMap.put("auditorEmail", assignment.getAuditorEmail());
            assMap.put("auditorType", assignment.getAuditorType());
            assMap.put("auditCategory", assignment.getCategory());
            assMap.put("post", assignment.getPost());
            assMap.put("status", assignment.getStatus().toLowerCase());
            assMap.put("submittedAt", assignment.getSubmittedAt() != null ? assignment.getSubmittedAt().toString() : null);
            
            try {
                assMap.put("values", assignment.getValuesData() != null ? mapper.readTree(assignment.getValuesData()) : mapper.createObjectNode());
            } catch (Exception e) {
                assMap.put("values", mapper.createObjectNode());
            }
            try {
                assMap.put("attachments", assignment.getAttachments() != null ? mapper.readTree(assignment.getAttachments()) : mapper.createArrayNode());
            } catch (Exception e) {
                assMap.put("attachments", mapper.createArrayNode());
            }
            assignmentsList.add(assMap);
            
            if ("administrative".equalsIgnoreCase(submission.getAuditType()) && assignment.getPost() != null && assignment.getSubmissionId().equals(submission.getId())) {
                String p = assignment.getPost();
                java.util.Map<String, Object> postStat = byPostMap.get(p);
                if (postStat == null) {
                    postStat = new java.util.HashMap<>();
                    postStat.put("post", p);
                    postStat.put("total", 0);
                    postStat.put("submitted", 0);
                    postStat.put("pending", 0);
                    byPostMap.put(p, postStat);
                }
                postStat.put("total", (int) postStat.get("total") + 1);
                if (isSub) {
                    postStat.put("submitted", (int) postStat.get("submitted") + 1);
                } else {
                    postStat.put("pending", (int) postStat.get("pending") + 1);
                }
            }
        }
        
        int pending = total - submitted;
        boolean allSubmitted = (total > 0 && pending == 0);
        
        submission.setAllAuditorsSubmitted(allSubmitted);
        submission.setAuditorAssignments(assignmentsList);
        
        java.util.Map<String, Object> progress = new java.util.HashMap<>();
        progress.put("total", total);
        progress.put("submitted", submitted);
        progress.put("pending", pending);
        progress.put("allSubmitted", allSubmitted);
        progress.put("byPost", new java.util.ArrayList<>(byPostMap.values()));
        
        submission.setAuditorProgress(progress);
    }

    private LocalDateTime parseDateTime(String str) {
        if (str == null || str.isBlank()) return null;
        try {
            return LocalDateTime.parse(str);
        } catch (Exception e) {
            try {
                return java.time.ZonedDateTime.parse(str).toLocalDateTime();
            } catch (Exception ex) {
                return LocalDateTime.now();
            }
        }
    }

    @Transactional
    public void handleAuditorDeletionCleanup(Long auditorId) {
        List<SubmissionAuditorAssignment> deletedAssignments = auditorAssignmentRepository.findByAuditorId(auditorId);
        if (deletedAssignments.isEmpty()) {
            return;
        }
        
        ObjectMapper mapper = new ObjectMapper();
        
        // Group assignments by submissionId so we can process each submission once
        java.util.Map<Long, java.util.List<SubmissionAuditorAssignment>> bySubmission = deletedAssignments.stream()
                .collect(java.util.stream.Collectors.groupingBy(SubmissionAuditorAssignment::getSubmissionId));
                
        for (java.util.Map.Entry<Long, java.util.List<SubmissionAuditorAssignment>> entry : bySubmission.entrySet()) {
            Long submissionId = entry.getKey();
            java.util.List<SubmissionAuditorAssignment> auditorAssignmentsForSub = entry.getValue();
            
            Submission submission = submissionRepository.findByIdForUpdate(submissionId).orElse(null);
            if (submission == null) {
                continue;
            }
            
            String auditType = submission.getAuditType();
            
            // Clean up valuesData, tablesData and attachments on the main submission
            try {
                com.fasterxml.jackson.databind.node.ObjectNode valuesNode = objectNodeOrEmpty(mapper, submission.getValuesData());
                com.fasterxml.jackson.databind.node.ObjectNode tablesNode = objectNodeOrEmpty(mapper, submission.getTablesData());
                
                for (SubmissionAuditorAssignment assignment : auditorAssignmentsForSub) {
                    if ("administrative".equalsIgnoreCase(auditType)) {
                        String post = assignment.getPost();
                        String canonicalPost = canonicalAdministrativePost(post);
                        
                        if (canonicalPost != null) {
                            // Remove all fields in valuesData that belong to this post
                            java.util.List<String> keysToRemove = new java.util.ArrayList<>();
                            valuesNode.fieldNames().forEachRemaining(key -> {
                                if (canonicalPost.equalsIgnoreCase(canonicalAdministrativePost(resolvePostForKey(key)))) {
                                    keysToRemove.add(key);
                                }
                            });
                            keysToRemove.forEach(valuesNode::remove);
                            
                            // Remove all tables in tablesData that belong to this post
                            java.util.List<String> tablesToRemove = new java.util.ArrayList<>();
                            tablesNode.fieldNames().forEachRemaining(key -> {
                                if (canonicalPost.equalsIgnoreCase(canonicalAdministrativePost(resolvePostForKey(key)))) {
                                    keysToRemove.add(key);
                                }
                            });
                            tablesToRemove.forEach(tablesNode::remove);
                        }
                    } else {
                        // Academic: clear Part E fields
                        java.util.List<String> keysToRemove = new java.util.ArrayList<>();
                        valuesNode.fieldNames().forEachRemaining(key -> {
                            if ("E".equalsIgnoreCase(classifyAdministrativeValueSection(key)) || key.toLowerCase().contains("auditorsignoff")) {
                                keysToRemove.add(key);
                            }
                        });
                        keysToRemove.forEach(valuesNode::remove);
                        
                        java.util.List<String> tablesToRemove = new java.util.ArrayList<>();
                        tablesNode.fieldNames().forEachRemaining(key -> {
                            if ("E".equalsIgnoreCase(classifyAdministrativeTableSection(key))) {
                                tablesToRemove.add(key);
                            }
                        });
                        tablesToRemove.forEach(tablesNode::remove);
                    }
                }
                
                submission.setValuesData(mapper.writeValueAsString(valuesNode));
                submission.setTablesData(mapper.writeValueAsString(tablesNode));
            } catch (Exception e) {
                System.err.println("Error cleaning up auditor data from submission: " + e.getMessage());
            }
            
            // Delete these assignments from database now
            for (SubmissionAuditorAssignment assignment : auditorAssignmentsForSub) {
                auditorAssignmentRepository.delete(assignment);
            }
            
            // Re-evaluate submission status based on remaining assignments
            List<SubmissionAuditorAssignment> remainingAssignments = auditorAssignmentRepository.findBySubmissionIdAndAuditorType(submissionId, submission.getForwardedAuditorType());
            if (remainingAssignments.isEmpty()) {
                remainingAssignments = auditorAssignmentRepository.findBySubmissionId(submissionId);
            }
            
            java.util.List<SubmissionAuditorAssignment> validRemainingAssignments = remainingAssignments;
            if ("administrative".equalsIgnoreCase(submission.getAuditType())) {
                java.util.Set<String> validAdminPosts = java.util.Set.of("registrar", "hr", "dean-placement", "dean-student-welfare");
                validRemainingAssignments = remainingAssignments.stream()
                        .filter(a -> {
                            String postCanonical = canonicalAdministrativePost(a.getPost());
                            return postCanonical != null && validAdminPosts.contains(postCanonical);
                        })
                        .collect(java.util.stream.Collectors.toList());
            }
            
            if (validRemainingAssignments.isEmpty()) {
                // No auditors left: revert to UNDER_REVIEW (removes it from IQAC completed dashboard)
                submission.setStatus("UNDER_REVIEW");
                submission.setAuditorReviewedBy(null);
                submission.setAuditorReviewedByDesignation(null);
                submission.setAuditorReviewedByRole(null);
                submission.setAuditorReviewedOn(null);
            } else {
                int total = validRemainingAssignments.size();
                int submitted = (int) validRemainingAssignments.stream().filter(a -> "SUBMITTED".equalsIgnoreCase(a.getStatus())).count();
                int pending = total - submitted;
                boolean allSubmitted = (pending == 0);
                
                if (allSubmitted) {
                    submission.setStatus("AUDITOR_COMPLETED");
                    SubmissionAuditorAssignment lastSub = validRemainingAssignments.stream()
                            .filter(a -> "SUBMITTED".equalsIgnoreCase(a.getStatus()))
                            .findFirst().orElse(null);
                    if (lastSub != null) {
                        submission.setAuditorReviewedBy(lastSub.getAuditorName());
                        submission.setAuditorReviewedByRole("auditor");
                        submission.setAuditorReviewedOn(lastSub.getSubmittedAt());
                    }
                } else {
                    submission.setStatus("UNDER_REVIEW");
                    submission.setAuditorReviewedBy(null);
                    submission.setAuditorReviewedByDesignation(null);
                    submission.setAuditorReviewedByRole(null);
                    submission.setAuditorReviewedOn(null);
                }
            }
            
            submissionRepository.save(submission);
        }
    }

    private void populateNextCycleLinkage(Submission submission) {
        if (submission == null || submission.getId() == null) return;
        
        // We only care about approved internal reports (reportCategory = INTERNAL or version = 1)
        boolean isInternal = "INTERNAL".equalsIgnoreCase(submission.getReportCategory()) 
                || (submission.getVersion() != null && submission.getVersion() == 1);
        if (!isInternal) {
            return;
        }
        
        // Find if a successor cycle exists
        Optional<Submission> nextCycleOpt = submissionRepository.findByParentSubmissionId(submission.getId());
        if (nextCycleOpt.isEmpty() && submission.getRootSubmissionId() != null) {
            nextCycleOpt = submissionRepository.findByRootSubmissionIdAndVersion(submission.getRootSubmissionId(), 2);
        }
        
        if (nextCycleOpt.isPresent()) {
            Submission nextCycle = nextCycleOpt.get();
            
            // Populate fields on the source submission
            submission.setHasNextCycle(true);
            submission.setNextVersionId(nextCycle.getId());
            submission.nextCycleStarted = true;
            
            // Sync linkages on the external successor cycle if missing
            boolean nextModified = false;
            if (nextCycle.getPreviousApprovedSubmissionId() == null || !nextCycle.getPreviousApprovedSubmissionId().equals(submission.getId())) {
                nextCycle.setPreviousApprovedSubmissionId(submission.getId());
                nextModified = true;
            }
            if (nextCycle.getParentSubmissionId() == null || !nextCycle.getParentSubmissionId().equals(submission.getId())) {
                nextCycle.setParentSubmissionId(submission.getId());
                nextModified = true;
            }
            Long expectedRootId = submission.getRootSubmissionId() != null ? submission.getRootSubmissionId() : submission.getId();
            if (nextCycle.getRootSubmissionId() == null || !nextCycle.getRootSubmissionId().equals(expectedRootId)) {
                nextCycle.setRootSubmissionId(expectedRootId);
                nextModified = true;
            }
            if (!"EXTERNAL".equalsIgnoreCase(nextCycle.getReportCategory())) {
                nextCycle.setReportCategory("EXTERNAL");
                nextModified = true;
            }
            if (nextModified) {
                submissionRepository.save(nextCycle);
            }
        }
    }
}
