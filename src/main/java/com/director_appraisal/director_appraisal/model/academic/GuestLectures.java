package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "guest_lectures")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestLectures {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(name = "resource_person", columnDefinition = "TEXT")
    private String nameOfTheResourcePerson;

    @Column(name = "designation_org", columnDefinition = "TEXT")
    private String designationAndOrganization;

    @Column(name = "conduction_date", columnDefinition = "TEXT")
    private String dateOfConduction;

    @Column(columnDefinition = "TEXT")
    private String topic;

    @Column(name = "no_beneficiaries", columnDefinition = "TEXT")
    private String numberOfBeneficiaries;

    @Column(name = "link_proof", columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
