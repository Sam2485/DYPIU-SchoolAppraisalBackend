package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.StudentPlacements;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentPlacementsRepository extends JpaRepository<StudentPlacements, Long> {
    List<StudentPlacements> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
