package com.director_appraisal.director_appraisal.repository.administrative;

import com.director_appraisal.director_appraisal.model.administrative.ScholarshipStudents;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScholarshipStudentsRepository extends JpaRepository<ScholarshipStudents, Long> {
    List<ScholarshipStudents> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
