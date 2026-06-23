package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "student_courses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourses {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(columnDefinition = "TEXT")
    private String nameOfStudent;

    @Column(columnDefinition = "TEXT")
    private String yearOfStudy;

    @Column(columnDefinition = "TEXT")
    private String nameOfCourse;

    @Column(columnDefinition = "TEXT")
    private String duration;

    @Column(columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
