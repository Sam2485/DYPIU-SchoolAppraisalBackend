package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.StudentPlacements;
import com.director_appraisal.director_appraisal.repository.academic.StudentPlacementsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentPlacementsService {

    private final StudentPlacementsRepository repository;

    public List<StudentPlacements> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<StudentPlacements> saveAll(Long submissionId, List<StudentPlacements> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (StudentPlacements row : rows) {
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
