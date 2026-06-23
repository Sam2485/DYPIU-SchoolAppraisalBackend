package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "research_funds")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchFunds {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(columnDefinition = "TEXT")
    private String nameOfTheProjectEndowmentsChairs;

    @Column(columnDefinition = "TEXT")
    private String nameOfThePrincipalInvestigator;

    @Column(columnDefinition = "TEXT")
    private String departmentOfPrincipalInvestigator;

    @Column(columnDefinition = "TEXT")
    private String yearOfAward;

    @Column(columnDefinition = "TEXT")
    private String fundsProvided;

    @Column(columnDefinition = "TEXT")
    private String durationOfTheProject;

    @Column(columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
