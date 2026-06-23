package com.director_appraisal.director_appraisal.service.administrative;

import com.director_appraisal.director_appraisal.model.administrative.CoursesOffered;
import com.director_appraisal.director_appraisal.repository.administrative.CoursesOfferedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CoursesOfferedService {

    private final CoursesOfferedRepository repository;

    public List<CoursesOffered> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<CoursesOffered> saveAll(Long submissionId, List<CoursesOffered> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (CoursesOffered row : rows) {
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
