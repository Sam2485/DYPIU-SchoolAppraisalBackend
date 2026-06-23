package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.CorporateTraining;
import com.director_appraisal.director_appraisal.repository.academic.CorporateTrainingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CorporateTrainingService {

    private final CorporateTrainingRepository repository;

    public List<CorporateTraining> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<CorporateTraining> saveAll(Long submissionId, List<CorporateTraining> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (CorporateTraining row : rows) {
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
