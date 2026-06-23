package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "fdp_attended")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FdpAttended {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String slNo;

    @Column(columnDefinition = "TEXT")
    private String nameOfFaculty;

    @Column(columnDefinition = "TEXT")
    private String titleOfSeminarCourse;

    @Column(columnDefinition = "TEXT")
    private String sponsoringAgencyOrganization;

    @Column(columnDefinition = "TEXT")
    private String durationWithDates;

    @Column(columnDefinition = "TEXT")
    private String date;

    @Column(columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
