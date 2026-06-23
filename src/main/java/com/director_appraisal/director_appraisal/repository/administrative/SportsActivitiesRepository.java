package com.director_appraisal.director_appraisal.repository.administrative;

import com.director_appraisal.director_appraisal.model.administrative.SportsActivities;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SportsActivitiesRepository extends JpaRepository<SportsActivities, Long> {
    List<SportsActivities> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
