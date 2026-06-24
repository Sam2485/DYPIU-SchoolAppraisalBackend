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

    @Column(name = "teacher_name", columnDefinition = "TEXT")
    private String nameOfTheTeacher;

    @Column(name = "module_name", columnDefinition = "TEXT")
    private String nameOfTheModule;

    @Column(name = "platform", columnDefinition = "TEXT")
    private String platformOnWhichModuleIsDeveloped;

    @Column(name = "launch_date", columnDefinition = "TEXT")
    private String dateOfLaunchingEContent;

    @Column(name = "link_proof", columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
