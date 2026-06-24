package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "student_awards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAwards {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(name = "student_name", columnDefinition = "TEXT")
    private String nameOfTheStudent;

    @Column(name = "award_details", columnDefinition = "TEXT")
    private String detailsOfTheAward;

    @Column(name = "proof_attachment", columnDefinition = "TEXT")
    private String proofAsAnAttachment;

}
