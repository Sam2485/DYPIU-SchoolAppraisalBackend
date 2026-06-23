package com.director_appraisal.director_appraisal.model.administrative;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "scholarship_summary")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScholarshipSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(columnDefinition = "TEXT")
    private String year;

    @Column(columnDefinition = "TEXT")
    private String titleOfScholarship;

    @Column(columnDefinition = "TEXT")
    private String numberOfTheStudents;

    @Column(columnDefinition = "TEXT")
    private String amountReceived;

    @Column(columnDefinition = "TEXT")
    private String awardingAgency;

    @Column(columnDefinition = "TEXT")
    private String awardingOrganization;

}
