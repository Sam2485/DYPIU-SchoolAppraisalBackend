package com.director_appraisal.director_appraisal.model.administrative;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "staff_training")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffTraining {

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

    @Column(columnDefinition = "TEXT")
    private String attachment;

}
