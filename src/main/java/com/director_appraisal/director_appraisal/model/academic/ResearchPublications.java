package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "research_publications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchPublications {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String titleOfPaper;

    @Column(columnDefinition = "TEXT")
    private String nameOfAuthor;

    @Column(columnDefinition = "TEXT")
    private String nameOfJournal;

    @Column(columnDefinition = "TEXT")
    private String yearOfPublicationWithVolumeAndPage;

    @Column(columnDefinition = "TEXT")
    private String isbnIssn;

    @Column(columnDefinition = "TEXT")
    private String indicateUgcApprovedJournal;

    @Column(columnDefinition = "TEXT")
    private String nationalInternationalJournal;

    @Column(columnDefinition = "TEXT")
    private String impactFactor;

    @Column(columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
