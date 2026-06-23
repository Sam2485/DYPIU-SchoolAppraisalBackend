package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.TeacherAwards;
import com.director_appraisal.director_appraisal.repository.academic.TeacherAwardsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherAwardsService {

    private final TeacherAwardsRepository repository;

    public List<TeacherAwards> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<TeacherAwards> saveAll(Long submissionId, List<TeacherAwards> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (TeacherAwards row : rows) {
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
