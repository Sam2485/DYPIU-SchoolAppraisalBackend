package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.SwocStrength;
import com.director_appraisal.director_appraisal.repository.academic.SwocStrengthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SwocStrengthService {

    private final SwocStrengthRepository repository;

    public List<SwocStrength> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<SwocStrength> saveAll(Long submissionId, List<SwocStrength> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (SwocStrength row : rows) {
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
