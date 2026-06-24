package com.director_appraisal.director_appraisal.model.administrative;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "industry_collaborations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndustryCollaborations {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(name = "partner_org", columnDefinition = "TEXT")
    private String nameOfTheOrganizationInstitutionIndustryWithWhomMouIsSigned;

    @Column(name = "signing_year", columnDefinition = "TEXT")
    private String yearOfSigningMou;

    @Column(name = "mou_duration", columnDefinition = "TEXT")
    private String durationOfMou;

    @Column(name = "activities", columnDefinition = "TEXT")
    private String listTheActualActivitiesUnderEachMou;

}
