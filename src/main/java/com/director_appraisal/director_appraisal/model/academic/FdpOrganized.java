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

    @Column(columnDefinition = "TEXT")
    private String nameOfConvenerCoordinator;

    @Column(columnDefinition = "TEXT")
    private String titleOfSeminarCourse;

    @Column(columnDefinition = "TEXT")
    private String sponsoringAgency;

    @Column(columnDefinition = "TEXT")
    private String durationWithDates;

    @Column(columnDefinition = "TEXT")
    private String noOfInternalAndExternalParticipants;

    @Column(columnDefinition = "TEXT")
    private String proceedingsPublishedYesNo;

    @Column(columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
