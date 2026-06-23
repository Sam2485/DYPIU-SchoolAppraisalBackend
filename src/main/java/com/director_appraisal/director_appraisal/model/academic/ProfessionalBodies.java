package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "professional_bodies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalBodies {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(columnDefinition = "TEXT")
    private String nameOfTheProfessionalBodyChapterStudentClub;

    @Column(columnDefinition = "TEXT")
    private String noOfStudentMembers;

    @Column(columnDefinition = "TEXT")
    private String dateOfEventConduction;

    @Column(columnDefinition = "TEXT")
    private String titleOfTheEvent;

    @Column(columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
