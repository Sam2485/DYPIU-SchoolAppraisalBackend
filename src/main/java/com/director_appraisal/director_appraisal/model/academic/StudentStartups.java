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

    @Column(name = "student_name", columnDefinition = "TEXT")
    private String nameOfTheStudent;

    @Column(name = "venture_name", columnDefinition = "TEXT")
    private String nameOfTheVentureStartUp;

    @Column(name = "link_proof", columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
