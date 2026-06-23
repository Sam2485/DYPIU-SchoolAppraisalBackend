package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "patents_copyrights")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatentsCopyrights {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(columnDefinition = "TEXT")
    private String nameOfFacultyStudent;

    @Column(columnDefinition = "TEXT")
    private String applicationNo;

    @Column(columnDefinition = "TEXT")
    private String titleOfPatentCopyright;

    @Column(columnDefinition = "TEXT")
    private String dateOfFiling;

    @Column(columnDefinition = "TEXT")
    private String dateOfPublication;

    @Column(columnDefinition = "TEXT")
    private String dateOfAward;

    @Column(columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
