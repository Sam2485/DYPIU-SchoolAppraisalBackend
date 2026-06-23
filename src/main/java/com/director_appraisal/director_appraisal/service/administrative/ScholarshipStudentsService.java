package com.director_appraisal.director_appraisal.service.administrative;

import com.director_appraisal.director_appraisal.model.administrative.ScholarshipStudents;
import com.director_appraisal.director_appraisal.repository.administrative.ScholarshipStudentsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScholarshipStudentsService {

    private final ScholarshipStudentsRepository repository;

    public List<ScholarshipStudents> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<ScholarshipStudents> saveAll(Long submissionId, List<ScholarshipStudents> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (ScholarshipStudents row : rows) {
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
