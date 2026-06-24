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

    @Column(name = "teacher_name", columnDefinition = "TEXT")
    private String nameOfTheTeacher;

    @Column(name = "book_chapters_title", columnDefinition = "TEXT")
    private String titleOfTheBookChaptersPublished;

    @Column(name = "paper_title", columnDefinition = "TEXT")
    private String titleOfThePaper;

    @Column(name = "proceedings_title", columnDefinition = "TEXT")
    private String titleOfTheProceedingsOfTheConference;

    @Column(name = "conference_name", columnDefinition = "TEXT")
    private String nameOfTheConference;

    @Column(name = "scope", columnDefinition = "TEXT")
    private String nationalInternational;

    @Column(name = "publication_year", columnDefinition = "TEXT")
    private String yearOfPublication;

    @Column(name = "isbn_issn", columnDefinition = "TEXT")
    private String isbnIssnNumber;

    @Column(name = "publisher_name", columnDefinition = "TEXT")
    private String nameOfThePublisher;

    @Column(name = "link_proof", columnDefinition = "TEXT")
    private String linkToRelevantProof;

}
