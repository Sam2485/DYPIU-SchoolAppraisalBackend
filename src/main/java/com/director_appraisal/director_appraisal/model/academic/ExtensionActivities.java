package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "extension_activities")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtensionActivities {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(columnDefinition = "TEXT")
    private String activityDetails;

    @Column(columnDefinition = "TEXT")
    private String organizedBy;

    @Column(name = "conduction_date", columnDefinition = "TEXT")
    private String dateOfConduction;

    @Column(name = "no_beneficiaries", columnDefinition = "TEXT")
    private String numberOfBeneficiaries;

    @Column(name = "link_proof", columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
