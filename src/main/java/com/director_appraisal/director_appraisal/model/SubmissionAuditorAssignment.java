package com.director_appraisal.director_appraisal.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "submission_auditor_assignments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionAuditorAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long submissionId;
    private Long auditorId;
    private String auditorName;
    private String auditorEmail;
    private String auditorType;
    private String category;
    private LocalDateTime assignedAt;
    private String post;

    @jakarta.persistence.Transient
    public String getSchool() {
        if ("academic".equalsIgnoreCase(category)) {
            return post;
        }
        return null;
    }

    @jakarta.persistence.Transient
    public void setSchool(String school) {
        if ("academic".equalsIgnoreCase(category) || category == null) {
            this.post = school;
        }
    }

    @Builder.Default
    private String status = "PENDING";

    private LocalDateTime submittedAt;

    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String valuesData;

    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String tablesData;

    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String attachments;
}
