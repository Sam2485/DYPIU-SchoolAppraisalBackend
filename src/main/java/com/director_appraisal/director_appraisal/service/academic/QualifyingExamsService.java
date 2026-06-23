package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.QualifyingExams;
import com.director_appraisal.director_appraisal.repository.academic.QualifyingExamsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QualifyingExamsService {

    private final QualifyingExamsRepository repository;

    public List<QualifyingExams> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<QualifyingExams> saveAll(Long submissionId, List<QualifyingExams> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (QualifyingExams row : rows) {
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
