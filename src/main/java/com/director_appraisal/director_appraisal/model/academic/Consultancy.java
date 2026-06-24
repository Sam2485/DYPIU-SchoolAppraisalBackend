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

    @Column(name = "faculty_name", columnDefinition = "TEXT")
    private String nameOfTheFaculty;

    @Column(name = "project_title", columnDefinition = "TEXT")
    private String titleOfTheConsultancyProject;

    @Column(name = "sponsoring_agency", columnDefinition = "TEXT")
    private String consultingSponsoringAgency;

    @Column(columnDefinition = "TEXT")
    private String revenueGenerated;

    @Column(name = "link_proof", columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
