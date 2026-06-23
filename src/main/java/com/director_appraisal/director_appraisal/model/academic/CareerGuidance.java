package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "career_guidance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerGuidance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(columnDefinition = "TEXT")
    private String sessionDetails;

    @Column(columnDefinition = "TEXT")
    private String resourcePersonDetails;

    @Column(columnDefinition = "TEXT")
    private String dateOfConduction;

    @Column(columnDefinition = "TEXT")
    private String numberOfBeneficiaries;

    @Column(columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
