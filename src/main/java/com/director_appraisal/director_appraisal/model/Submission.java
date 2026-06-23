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
    private String status; // DRAFT, SUBMITTED, UNDER_REVIEW, APPROVED, SENT_BACK

    @Column(columnDefinition = "TEXT")
    private String remarks;

    private String reviewedBy;
    private LocalDateTime reviewedAt;

    @Column(columnDefinition = "TEXT")
    private String valuesData; // JSON string of field values

    @Column(columnDefinition = "TEXT")
    private String tablesData; // JSON string of table contents

    @Column(columnDefinition = "TEXT")
    private String attachments; // JSON string of attachments list

    @Version
    @Builder.Default
    private Integer version = 1;
}
