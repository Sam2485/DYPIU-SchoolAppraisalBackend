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
    private String submittedBy;
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
}
