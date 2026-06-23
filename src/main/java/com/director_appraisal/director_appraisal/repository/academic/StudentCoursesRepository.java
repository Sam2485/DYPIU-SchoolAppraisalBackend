package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.StudentCourses;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentCoursesRepository extends JpaRepository<StudentCourses, Long> {
    List<StudentCourses> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
