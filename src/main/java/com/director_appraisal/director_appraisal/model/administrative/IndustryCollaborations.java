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

    @Column(columnDefinition = "TEXT")
    private String nameOfTheOrganizationInstitutionIndustryWithWhomMouIsSigned;

    @Column(columnDefinition = "TEXT")
    private String yearOfSigningMou;

    @Column(columnDefinition = "TEXT")
    private String durationOfMou;

    @Column(columnDefinition = "TEXT")
    private String listTheActualActivitiesUnderEachMou;

}
