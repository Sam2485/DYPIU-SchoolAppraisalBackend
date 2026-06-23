package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.ValueAddedCourses;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ValueAddedCoursesRepository extends JpaRepository<ValueAddedCourses, Long> {
    List<ValueAddedCourses> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
