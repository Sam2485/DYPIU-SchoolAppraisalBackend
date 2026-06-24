package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "higher_studies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HigherStudies {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String program;

    @Column(name = "students_appeared", columnDefinition = "TEXT")
    private String noOfStudentsAppearedForFinalYearExam;

    @Column(name = "selected_students", columnDefinition = "TEXT")
    private String noOfStudentsSelectedForHigherStudies;

    @Column(columnDefinition = "TEXT")
    private String studentsPercent;

}
