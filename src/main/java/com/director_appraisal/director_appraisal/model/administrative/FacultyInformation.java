package com.director_appraisal.director_appraisal.model.administrative;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "faculty_information")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacultyInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(columnDefinition = "TEXT")
    private String cadre;

    @Column(columnDefinition = "TEXT")
    private String required;

    @Column(columnDefinition = "TEXT")
    private String regular;

    @Column(columnDefinition = "TEXT")
    private String contract;

}
