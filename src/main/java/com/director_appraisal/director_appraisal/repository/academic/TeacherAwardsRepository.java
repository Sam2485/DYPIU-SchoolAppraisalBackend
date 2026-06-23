package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.TeacherAwards;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TeacherAwardsRepository extends JpaRepository<TeacherAwards, Long> {
    List<TeacherAwards> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
