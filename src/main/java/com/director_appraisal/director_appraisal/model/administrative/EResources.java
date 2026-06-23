package com.director_appraisal.director_appraisal.model.administrative;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "e_resources")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EResources {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(columnDefinition = "TEXT")
    private String facilities;

    @Column(columnDefinition = "TEXT")
    private String availability;

    @Column(columnDefinition = "TEXT")
    private String remarks;

}
