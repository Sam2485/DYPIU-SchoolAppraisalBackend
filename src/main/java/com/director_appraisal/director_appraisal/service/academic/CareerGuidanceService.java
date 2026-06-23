package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.CareerGuidance;
import com.director_appraisal.director_appraisal.repository.academic.CareerGuidanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CareerGuidanceService {

    private final CareerGuidanceRepository repository;

    public List<CareerGuidance> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<CareerGuidance> saveAll(Long submissionId, List<CareerGuidance> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (CareerGuidance row : rows) {
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
