package com.director_appraisal.director_appraisal.model.academic;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "books_chapters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BooksChapters {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String nameOfTheTeacher;

    @Column(columnDefinition = "TEXT")
    private String titleOfTheBookChaptersPublished;

    @Column(columnDefinition = "TEXT")
    private String titleOfThePaper;

    @Column(columnDefinition = "TEXT")
    private String titleOfTheProceedingsOfTheConference;

    @Column(columnDefinition = "TEXT")
    private String nameOfTheConference;

    @Column(columnDefinition = "TEXT")
    private String nationalInternational;

    @Column(columnDefinition = "TEXT")
    private String yearOfPublication;

    @Column(columnDefinition = "TEXT")
    private String isbnIssnNumber;

    @Column(columnDefinition = "TEXT")
    private String nameOfThePublisher;

    @Column(columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
