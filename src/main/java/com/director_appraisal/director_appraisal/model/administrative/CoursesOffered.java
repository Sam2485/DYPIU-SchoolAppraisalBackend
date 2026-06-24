package com.director_appraisal.director_appraisal.model.administrative;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "courses_offered")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoursesOffered {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(name = "program_name", columnDefinition = "TEXT")
    private String nameOfTheProgram;

    @Column(columnDefinition = "TEXT")
    private String levelUgPg;

    @Column(columnDefinition = "TEXT")
    private String intake;

    @Column(name = "commencement_year", columnDefinition = "TEXT")
    private String yearOfCommencementOfTheProgram;

}
