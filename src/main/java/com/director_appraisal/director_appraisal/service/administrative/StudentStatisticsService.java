package com.director_appraisal.director_appraisal.service.administrative;

import com.director_appraisal.director_appraisal.model.administrative.StudentStatistics;
import com.director_appraisal.director_appraisal.repository.administrative.StudentStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentStatisticsService {

    private final StudentStatisticsRepository repository;

    public List<StudentStatistics> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<StudentStatistics> saveAll(Long submissionId, List<StudentStatistics> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (StudentStatistics row : rows) {
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
