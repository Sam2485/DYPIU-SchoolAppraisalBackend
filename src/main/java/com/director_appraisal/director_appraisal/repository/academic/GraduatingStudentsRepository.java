package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.GraduatingStudents;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GraduatingStudentsRepository extends JpaRepository<GraduatingStudents, Long> {
    List<GraduatingStudents> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
