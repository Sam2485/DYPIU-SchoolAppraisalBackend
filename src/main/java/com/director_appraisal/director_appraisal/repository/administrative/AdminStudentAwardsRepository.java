package com.director_appraisal.director_appraisal.repository.administrative;

import com.director_appraisal.director_appraisal.model.administrative.AdminStudentAwards;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdminStudentAwardsRepository extends JpaRepository<AdminStudentAwards, Long> {
    List<AdminStudentAwards> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
