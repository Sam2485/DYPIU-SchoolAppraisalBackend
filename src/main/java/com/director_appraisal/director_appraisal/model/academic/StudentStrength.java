package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "student_strength")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentStrength {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String className;

    @Column(columnDefinition = "TEXT")
    private String noOfStudents;

    @Column(columnDefinition = "TEXT")
    private String total;

}
