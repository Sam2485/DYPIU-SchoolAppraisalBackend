package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "nep_status")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NepStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String sn;

    @Column(columnDefinition = "TEXT")
    private String checkPoints;

    @Column(columnDefinition = "TEXT")
    private String availability;

    @Column(name = "link_document", columnDefinition = "TEXT")
    private String linkForTheDocument;

}
