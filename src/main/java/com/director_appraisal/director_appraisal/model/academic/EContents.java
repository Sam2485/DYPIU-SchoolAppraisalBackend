package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "e_contents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EContents {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(columnDefinition = "TEXT")
    private String nameOfTheTeacher;

    @Column(columnDefinition = "TEXT")
    private String nameOfTheModule;

    @Column(columnDefinition = "TEXT")
    private String platformOnWhichModuleIsDeveloped;

    @Column(columnDefinition = "TEXT")
    private String dateOfLaunchingEContent;

    @Column(columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
