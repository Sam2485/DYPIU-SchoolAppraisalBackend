package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.FacultyStrength;
import com.director_appraisal.director_appraisal.repository.academic.FacultyStrengthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FacultyStrengthService {

    private final FacultyStrengthRepository repository;

    public List<FacultyStrength> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<FacultyStrength> saveAll(Long submissionId, List<FacultyStrength> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (FacultyStrength row : rows) {
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
