package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "teacher_awards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAwards {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(name = "teacher_name", columnDefinition = "TEXT")
    private String nameOfTheTeacher;

    @Column(columnDefinition = "TEXT")
    private String nationalAwards;

    @Column(columnDefinition = "TEXT")
    private String internationalAwards;

    @Column(name = "link_proof", columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
