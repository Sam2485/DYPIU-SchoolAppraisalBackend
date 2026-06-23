package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.StudentStartups;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentStartupsRepository extends JpaRepository<StudentStartups, Long> {
    List<StudentStartups> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
