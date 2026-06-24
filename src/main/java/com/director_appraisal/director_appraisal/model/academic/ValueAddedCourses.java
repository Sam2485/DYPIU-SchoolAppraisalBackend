package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "value_added_courses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValueAddedCourses {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(name = "course_title", columnDefinition = "TEXT")
    private String titleOfTheCourse;

    @Column(name = "resource_person", columnDefinition = "TEXT")
    private String detailsOfResourcePerson;

    @Column(name = "duration_date", columnDefinition = "TEXT")
    private String durationAndDateOfConduction;

    @Column(columnDefinition = "TEXT")
    private String noOfBeneficiaries;

    @Column(name = "link_proof", columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
