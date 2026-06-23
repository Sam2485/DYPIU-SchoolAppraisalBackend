package com.director_appraisal.director_appraisal.repository.administrative;

import com.director_appraisal.director_appraisal.model.administrative.StudentStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentStatisticsRepository extends JpaRepository<StudentStatistics, Long> {
    List<StudentStatistics> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
