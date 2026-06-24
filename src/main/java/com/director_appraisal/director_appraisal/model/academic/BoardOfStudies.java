package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "board_of_studies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardOfStudies {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(name = "meeting_date", columnDefinition = "TEXT")
    private String dateOfTheMeeting;

    @Column(columnDefinition = "TEXT")
    private String linkForMom;

}
