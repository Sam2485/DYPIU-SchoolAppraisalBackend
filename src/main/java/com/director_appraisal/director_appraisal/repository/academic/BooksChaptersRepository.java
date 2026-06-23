package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.BooksChapters;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BooksChaptersRepository extends JpaRepository<BooksChapters, Long> {
    List<BooksChapters> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
