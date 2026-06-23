package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.StudentStartups;
import com.director_appraisal.director_appraisal.repository.academic.StudentStartupsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentStartupsService {

    private final StudentStartupsRepository repository;

    public List<StudentStartups> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<StudentStartups> saveAll(Long submissionId, List<StudentStartups> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (StudentStartups row : rows) {
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
