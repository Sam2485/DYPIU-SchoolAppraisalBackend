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

    @Column(name = "paper_title", columnDefinition = "TEXT")
    private String titleOfPaper;

    @Column(name = "author_name", columnDefinition = "TEXT")
    private String nameOfAuthor;

    @Column(name = "journal_name", columnDefinition = "TEXT")
    private String nameOfJournal;

    @Column(name = "publication_details", columnDefinition = "TEXT")
    private String yearOfPublicationWithVolumeAndPage;

    @Column(columnDefinition = "TEXT")
    private String isbnIssn;

    @Column(name = "ugc_approved", columnDefinition = "TEXT")
    private String indicateUgcApprovedJournal;

    @Column(name = "journal_type", columnDefinition = "TEXT")
    private String nationalInternationalJournal;

    @Column(columnDefinition = "TEXT")
    private String impactFactor;

    @Column(name = "link_proof", columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
