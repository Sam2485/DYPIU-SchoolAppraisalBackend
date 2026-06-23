package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.StudentAwards;
import com.director_appraisal.director_appraisal.repository.academic.StudentAwardsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentAwardsService {

    private final StudentAwardsRepository repository;

    public List<StudentAwards> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<StudentAwards> saveAll(Long submissionId, List<StudentAwards> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (StudentAwards row : rows) {
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
