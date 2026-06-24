package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "student_placements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentPlacements {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String program;

    @Column(name = "students_appeared", columnDefinition = "TEXT")
    private String noOfStudentsAppearedForFinalYearExam;

    @Column(name = "students_placed", columnDefinition = "TEXT")
    private String noOfStudentsPlaced;

    @Column(columnDefinition = "TEXT")
    private String placementPercent;

    @Column(name = "proof_attachment", columnDefinition = "TEXT")
    private String proofAsAttachment;

}
