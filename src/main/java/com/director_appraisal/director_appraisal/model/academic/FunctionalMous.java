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

    @Column(columnDefinition = "TEXT")
    private String nameOfTheOrganizationInstitutionIndustry;

    @Column(columnDefinition = "TEXT")
    private String yearOfSigningMou;

    @Column(columnDefinition = "TEXT")
    private String durationOfMou;

    @Column(columnDefinition = "TEXT")
    private String listTheActualActivitiesUnderEachMou;

    @Column(columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
