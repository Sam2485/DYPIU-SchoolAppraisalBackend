package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "success_rate")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuccessRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String program;

    @Column(columnDefinition = "TEXT")
    private String noOfStudentsAppearedForFinalSemesterExam;

    @Column(columnDefinition = "TEXT")
    private String numberOfStudentsClearedProgramInStipulatedDuration;

    @Column(columnDefinition = "TEXT")
    private String successRatePercent;

}
