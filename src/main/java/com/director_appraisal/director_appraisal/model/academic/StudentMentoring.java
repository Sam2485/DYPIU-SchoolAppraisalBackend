package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "student_mentoring")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentMentoring {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(name = "mentor_name", columnDefinition = "TEXT")
    private String nameOfMentor;

    @Column(columnDefinition = "TEXT")
    private String noOfMentees;

    @Column(columnDefinition = "TEXT")
    private String linkToDocument;

}
