package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.ExtensionActivities;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExtensionActivitiesRepository extends JpaRepository<ExtensionActivities, Long> {
    List<ExtensionActivities> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
