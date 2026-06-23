package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "qualifying_exams")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualifyingExams {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(columnDefinition = "TEXT")
    private String nameOfTheStudent;

    @Column(columnDefinition = "TEXT")
    private String examinationDetails;

    @Column(columnDefinition = "TEXT")
    private String proofAsAttachment;

}
