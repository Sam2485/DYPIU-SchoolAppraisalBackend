package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.StudentStrength;
import com.director_appraisal.director_appraisal.repository.academic.StudentStrengthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentStrengthService {

    private final StudentStrengthRepository repository;

    public List<StudentStrength> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<StudentStrength> saveAll(Long submissionId, List<StudentStrength> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (StudentStrength row : rows) {
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
