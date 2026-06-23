package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "corporate_training")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorporateTraining {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(columnDefinition = "TEXT")
    private String nameOfFaculty;

    @Column(columnDefinition = "TEXT")
    private String agencySeekingTraining;

    @Column(columnDefinition = "TEXT")
    private String revenueGenerated;

    @Column(columnDefinition = "TEXT")
    private String numberOfTrainees;

    @Column(columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
