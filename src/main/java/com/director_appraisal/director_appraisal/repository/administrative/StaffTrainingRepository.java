package com.director_appraisal.director_appraisal.repository.administrative;

import com.director_appraisal.director_appraisal.model.administrative.StaffTraining;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StaffTrainingRepository extends JpaRepository<StaffTraining, Long> {
    List<StaffTraining> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
