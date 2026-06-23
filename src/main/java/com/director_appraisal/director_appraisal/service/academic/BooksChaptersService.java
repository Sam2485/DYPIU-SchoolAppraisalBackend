package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.BooksChapters;
import com.director_appraisal.director_appraisal.repository.academic.BooksChaptersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BooksChaptersService {

    private final BooksChaptersRepository repository;

    public List<BooksChapters> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<BooksChapters> saveAll(Long submissionId, List<BooksChapters> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (BooksChapters row : rows) {
            row.setId(null);
            row.setSubmissionId(submissionId);
        }
        return repository.saveAll(rows);
    }

    @Transactional
    public void deleteBySubmissionId(Long submissionId) {
        repository.deleteBySubmissionId(submissionId);
    }
}
