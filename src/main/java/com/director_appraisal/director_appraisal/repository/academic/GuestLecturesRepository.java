package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.GuestLectures;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GuestLecturesRepository extends JpaRepository<GuestLectures, Long> {
    List<GuestLectures> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
