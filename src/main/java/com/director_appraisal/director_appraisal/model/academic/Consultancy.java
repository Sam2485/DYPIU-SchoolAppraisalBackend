package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "consultancy")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Consultancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String nameOfTheFaculty;

    @Column(columnDefinition = "TEXT")
    private String titleOfTheConsultancyProject;

    @Column(columnDefinition = "TEXT")
    private String consultingSponsoringAgency;

    @Column(columnDefinition = "TEXT")
    private String revenueGenerated;

    @Column(columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
