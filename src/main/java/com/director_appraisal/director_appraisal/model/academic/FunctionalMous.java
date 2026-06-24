package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "functional_mous")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionalMous {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(name = "partner_org", columnDefinition = "TEXT")
    private String nameOfTheOrganizationInstitutionIndustry;

    @Column(name = "signing_year", columnDefinition = "TEXT")
    private String yearOfSigningMou;

    @Column(name = "mou_duration", columnDefinition = "TEXT")
    private String durationOfMou;

    @Column(name = "activities", columnDefinition = "TEXT")
    private String listTheActualActivitiesUnderEachMou;

    @Column(name = "link_proof", columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
