package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.SuccessRate;
import com.director_appraisal.director_appraisal.repository.academic.SuccessRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SuccessRateService {

    private final SuccessRateRepository repository;

    public List<SuccessRate> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<SuccessRate> saveAll(Long submissionId, List<SuccessRate> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (SuccessRate row : rows) {
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
