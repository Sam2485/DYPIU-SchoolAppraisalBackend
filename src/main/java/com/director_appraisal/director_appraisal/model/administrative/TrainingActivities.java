package com.director_appraisal.director_appraisal.model.administrative;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "training_activities")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingActivities {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(columnDefinition = "TEXT")
    private String academicYear;

    @Column(name = "event_name", columnDefinition = "TEXT")
    private String titleOfTheEvent;

    @Column(name = "conduction_date", columnDefinition = "TEXT")
    private String dateOfConduction;

    @Column(name = "students_benefited", columnDefinition = "TEXT")
    private String noOfStudentsBenefited;

}
