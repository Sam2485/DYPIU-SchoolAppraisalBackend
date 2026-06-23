package com.director_appraisal.director_appraisal.model.administrative;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "student_statistics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String srNo;

    @Column(columnDefinition = "TEXT")
    private String category;

    @Column(columnDefinition = "TEXT")
    private String ug;

    @Column(columnDefinition = "TEXT")
    private String pg;

    @Column(columnDefinition = "TEXT")
    private String phd;

    @Column(columnDefinition = "TEXT")
    private String valueAddedSkillCourses;

}
