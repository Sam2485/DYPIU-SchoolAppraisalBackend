package com.director_appraisal.director_appraisal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String auditType; // academic, administrative

    private String school;
    
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String submittedBy;
    
    @Column(columnDefinition = "TEXT")
    private String submittedByDetails;

    private LocalDateTime submittedAt;

    @Column(nullable = false)
    private String status; // DRAFT, SUBMITTED, UNDER_REVIEW, AUDITOR_COMPLETED, APPROVED, FINAL

    @Column(columnDefinition = "TEXT")
    private String remarks;

    private String reviewedBy;
    private LocalDateTime reviewedAt;

    // Legacy singular auditor fields
    private Long forwardedToAuditorId;
    private String forwardedToAuditorName;
    private String forwardedToAuditorEmail;

    // Plural auditor fields (storing JSON arrays of IDs, names, emails)
    @Column(columnDefinition = "TEXT")
    private String forwardedToAuditorIds;
    @Column(columnDefinition = "TEXT")
    private String forwardedToAuditorNames;
    @Column(columnDefinition = "TEXT")
    private String forwardedToAuditorEmails;

    // Forwarding details
    private String forwardedAuditorType; // internal | external
    private String forwardedAuditCategory; // academic | administrative
    private LocalDateTime forwardedAt;

    // Auditor review stamps
    private String auditorReviewedBy;
    private String auditorReviewedByDesignation;
    private String auditorReviewedByRole;
    private LocalDateTime auditorReviewedOn;

    private Long rootSubmissionId;
    private Long parentSubmissionId;
    private Long previousApprovedSubmissionId;
    private String academicYear;
    private String auditCycle;
    private String reportCategory;
    private String schoolGroup;
    private String administrativePost;
    private LocalDateTime approvedAt;
    private Long approvedByUserId;
    private String approvedByName;
    private String approvedByRole;
    private String approvedByDesignation;
    private Integer createdFromVersion;

    @Column(columnDefinition = "TEXT")
    private String valuesData; // JSON string of field values

    @Column(columnDefinition = "TEXT")
    private String tablesData; // JSON string of table contents

    @Column(columnDefinition = "TEXT")
    private String attachments; // JSON string of attachments list

    @Builder.Default
    private Integer version = 1;

    @Builder.Default
    private Boolean hasNextCycle = false;

    private Long nextVersionId;

    @Transient
    private java.util.Map<String, Object> permissions;

    @Column(columnDefinition = "TEXT")
    private String forwardedAdministrativePosts;

    @Column(columnDefinition = "TEXT")
    private String forwardedToAuditorPosts;

    @com.fasterxml.jackson.annotation.JsonGetter("auditType")
    public String getAuditTypeForJson() {
        return auditType != null ? auditType.toUpperCase() : null;
    }

    @com.fasterxml.jackson.annotation.JsonGetter("reportCategory")
    public String getReportCategoryForJson() {
        return reportCategory != null ? reportCategory.toUpperCase() : null;
    }

    @com.fasterxml.jackson.annotation.JsonGetter("academicYear")
    public String getAcademicYearForJson() {
        return academicYear != null ? academicYear : auditCycle;
    }

    @com.fasterxml.jackson.annotation.JsonGetter("schoolGroup")
    public String getSchoolGroupForJson() {
        return schoolGroup;
    }

    @com.fasterxml.jackson.annotation.JsonGetter("hasNextCycle")
    public boolean getHasNextCycleForJson() {
        return hasNextCycle != null && hasNextCycle;
    }

    @com.fasterxml.jackson.annotation.JsonGetter("overallStatus")
    public String getOverallStatusForJson() {
        if (!"administrative".equalsIgnoreCase(auditType)) {
            return status;
        }
        if ("SUBMITTED".equalsIgnoreCase(status)) {
            return "SUBMITTED";
        }
        java.util.Map<String, String> progress = getAdministrativeProgressForJson();
        boolean hasProgress = progress.values().stream().anyMatch(value -> !"DRAFT".equalsIgnoreCase(value));
        return hasProgress ? "IN_PROGRESS" : "DRAFT";
    }

    @com.fasterxml.jackson.annotation.JsonGetter("administrativeProgress")
    public java.util.Map<String, String> getAdministrativeProgressForJson() {
        java.util.Map<String, String> progress = new java.util.LinkedHashMap<>();
        progress.put("registrar", "DRAFT");
        progress.put("hr", "DRAFT");
        progress.put("dean-student-welfare", "DRAFT");
        progress.put("dean-placement", "DRAFT");
        if (valuesData == null || valuesData.isBlank()) {
            return progress;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(valuesData)
                    .get("administrativeProgress");
            if (node != null && node.isObject()) {
                progress.replaceAll((post, value) -> node.path(post).asText(value));
            }
        } catch (Exception ignored) {
            return progress;
        }
        return progress;
    }

    @com.fasterxml.jackson.annotation.JsonGetter("submittedBy")
    public Object getSubmittedByForJson() {
        if ("administrative".equalsIgnoreCase(auditType)) {
            if (submittedByDetails != null && !submittedByDetails.isBlank()) {
                try {
                    return new com.fasterxml.jackson.databind.ObjectMapper().readTree(submittedByDetails);
                } catch (Exception ignored) {}
            }
            return defaultSubmittedByDetails();
        }
        return submittedBy;
    }

    private Object defaultSubmittedByDetails() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode node = mapper.createObjectNode();
        
        String[] keys = {"registrar", "hr", "deanStudentWelfare", "deanPlacement"};
        for (String key : keys) {
            com.fasterxml.jackson.databind.node.ObjectNode roleNode = mapper.createObjectNode();
            roleNode.put("submitted", false);
            roleNode.putNull("submittedAt");
            roleNode.putNull("name");
            roleNode.putNull("email");
            node.set(key, roleNode);
        }
        return node;
    }

    public String getValuesData() {
        return com.director_appraisal.director_appraisal.util.UrlPostProcessor.process(valuesData);
    }

    public String getTablesData() {
        return com.director_appraisal.director_appraisal.util.UrlPostProcessor.process(tablesData);
    }

    public String getAttachments() {
        return com.director_appraisal.director_appraisal.util.UrlPostProcessor.process(attachments);
    }

    @com.fasterxml.jackson.annotation.JsonGetter("forwardedAdministrativePosts")
    public Object getForwardedAdministrativePostsForJson() {
        if (forwardedAdministrativePosts != null && !forwardedAdministrativePosts.isBlank()) {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(forwardedAdministrativePosts, java.util.List.class);
            } catch (Exception ignored) {}
        }
        return java.util.List.of();
    }

    @com.fasterxml.jackson.annotation.JsonGetter("forwardedToAuditorPosts")
    public Object getForwardedToAuditorPostsForJson() {
        if (forwardedToAuditorPosts != null && !forwardedToAuditorPosts.isBlank()) {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(forwardedToAuditorPosts, java.util.List.class);
            } catch (Exception ignored) {}
        }
        return java.util.List.of();
    }
}
