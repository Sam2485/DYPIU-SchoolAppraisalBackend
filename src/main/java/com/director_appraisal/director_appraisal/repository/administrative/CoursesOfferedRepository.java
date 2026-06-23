package com.director_appraisal.director_appraisal.repository.administrative;

import com.director_appraisal.director_appraisal.model.administrative.CoursesOffered;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CoursesOfferedRepository extends JpaRepository<CoursesOffered, Long> {
    List<CoursesOffered> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
