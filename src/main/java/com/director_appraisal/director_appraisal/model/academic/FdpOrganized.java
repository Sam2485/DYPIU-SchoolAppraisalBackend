package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "fdp_organized")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FdpOrganized {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String slNo;

    @Column(name = "coordinator", columnDefinition = "TEXT")
    private String nameOfConvenerCoordinator;

    @Column(name = "seminar_title", columnDefinition = "TEXT")
    private String titleOfSeminarCourse;

    @Column(columnDefinition = "TEXT")
    private String sponsoringAgency;

    @Column(name = "duration_dates", columnDefinition = "TEXT")
    private String durationWithDates;

    @Column(name = "participants_count", columnDefinition = "TEXT")
    private String noOfInternalAndExternalParticipants;

    @Column(name = "published", columnDefinition = "TEXT")
    private String proceedingsPublishedYesNo;

    @Column(name = "link_proof", columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
