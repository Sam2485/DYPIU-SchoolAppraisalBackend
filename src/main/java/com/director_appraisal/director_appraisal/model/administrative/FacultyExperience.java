package com.director_appraisal.director_appraisal.model.administrative;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "faculty_experience")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacultyExperience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String sNo;

    @Column(columnDefinition = "TEXT")
    private String nameOfTheFaculty;

    @Column(columnDefinition = "TEXT")
    private String designation;

    @Column(columnDefinition = "TEXT")
    private String highestQualification;

    @Column(columnDefinition = "TEXT")
    private String dateOfJoining;

    @Column(columnDefinition = "TEXT")
    private String experienceInDypiu;

    @Column(columnDefinition = "TEXT")
    private String experienceBeforeJoiningDypiu;

    @Column(columnDefinition = "TEXT")
    private String totalExperience;

}
