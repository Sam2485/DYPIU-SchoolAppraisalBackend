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
}
