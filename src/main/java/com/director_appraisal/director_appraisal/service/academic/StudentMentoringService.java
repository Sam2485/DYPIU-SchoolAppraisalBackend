package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.StudentMentoring;
import com.director_appraisal.director_appraisal.repository.academic.StudentMentoringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentMentoringService {

    private final StudentMentoringRepository repository;

    public List<StudentMentoring> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<StudentMentoring> saveAll(Long submissionId, List<StudentMentoring> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (StudentMentoring row : rows) {
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
