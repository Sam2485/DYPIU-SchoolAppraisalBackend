package com.director_appraisal.director_appraisal.model.administrative;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "statutory_bodies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatutoryBodies {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(columnDefinition = "TEXT")
    private String bodyCell;

    @Column(columnDefinition = "TEXT")
    private String meetingsConducted;

    @Column(columnDefinition = "TEXT")
    private String atrStatus;

    @Column(columnDefinition = "TEXT")
    private String remarksLink;

}
