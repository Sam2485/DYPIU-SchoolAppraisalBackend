package com.director_appraisal.director_appraisal.model.administrative;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "admin_student_awards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStudentAwards {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(name = "award_name", columnDefinition = "TEXT")
    private String nameOfTheAward;

    @Column(columnDefinition = "TEXT")
    private String teamIndividual;

    @Column(name = "level_type", columnDefinition = "TEXT")
    private String interUniversityStateNationalInternational;

    @Column(name = "event_name", columnDefinition = "TEXT")
    private String nameOfTheEvent;

    @Column(name = "student_name", columnDefinition = "TEXT")
    private String nameOfTheStudent;

    @Column(columnDefinition = "TEXT")
    private String attachment;

}
