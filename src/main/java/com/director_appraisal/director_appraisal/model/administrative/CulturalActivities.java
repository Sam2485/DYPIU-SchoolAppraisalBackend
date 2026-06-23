package com.director_appraisal.director_appraisal.model.administrative;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cultural_activities")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CulturalActivities {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(columnDefinition = "TEXT")
    private String activityDetails;

    @Column(columnDefinition = "TEXT")
    private String organizedBy;

    @Column(columnDefinition = "TEXT")
    private String dateOfConduction;

    @Column(columnDefinition = "TEXT")
    private String numberOfBeneficiariesParticipants;

}
