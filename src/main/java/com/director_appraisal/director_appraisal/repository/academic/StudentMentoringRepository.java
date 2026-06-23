package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.StudentMentoring;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentMentoringRepository extends JpaRepository<StudentMentoring, Long> {
    List<StudentMentoring> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
