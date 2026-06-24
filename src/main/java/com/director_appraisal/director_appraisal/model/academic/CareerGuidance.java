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

    @Column(name = "resource_person", columnDefinition = "TEXT")
    private String resourcePersonDetails;

    @Column(name = "conduction_date", columnDefinition = "TEXT")
    private String dateOfConduction;

    @Column(name = "no_beneficiaries", columnDefinition = "TEXT")
    private String numberOfBeneficiaries;

    @Column(name = "link_proof", columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
