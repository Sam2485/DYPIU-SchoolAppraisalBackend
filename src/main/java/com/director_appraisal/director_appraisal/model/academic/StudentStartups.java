package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "student_startups")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentStartups {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String sn;

    @Column(columnDefinition = "TEXT")
    private String nameOfTheStudent;

    @Column(columnDefinition = "TEXT")
    private String nameOfTheVentureStartUp;

    @Column(columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
