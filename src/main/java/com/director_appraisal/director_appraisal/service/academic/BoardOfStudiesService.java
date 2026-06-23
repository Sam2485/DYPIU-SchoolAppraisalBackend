package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.BoardOfStudies;
import com.director_appraisal.director_appraisal.repository.academic.BoardOfStudiesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardOfStudiesService {

    private final BoardOfStudiesRepository repository;

    public List<BoardOfStudies> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<BoardOfStudies> saveAll(Long submissionId, List<BoardOfStudies> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (BoardOfStudies row : rows) {
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
