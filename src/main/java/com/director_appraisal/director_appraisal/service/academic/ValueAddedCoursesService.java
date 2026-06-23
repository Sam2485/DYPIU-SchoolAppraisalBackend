package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.ValueAddedCourses;
import com.director_appraisal.director_appraisal.repository.academic.ValueAddedCoursesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ValueAddedCoursesService {

    private final ValueAddedCoursesRepository repository;

    public List<ValueAddedCourses> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<ValueAddedCourses> saveAll(Long submissionId, List<ValueAddedCourses> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (ValueAddedCourses row : rows) {
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
