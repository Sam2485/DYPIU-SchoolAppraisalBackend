package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "graduating_students")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraduatingStudents {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String program;

    @Column(columnDefinition = "TEXT")
    private String female;

    @Column(columnDefinition = "TEXT")
    private String male;

    @Column(columnDefinition = "TEXT")
    private String total;

}
