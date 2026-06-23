package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "faculty_strength")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacultyStrength {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String requiredFaculty;

    @Column(columnDefinition = "TEXT")
    private String available;

}
