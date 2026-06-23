package com.director_appraisal.director_appraisal.repository.administrative;

import com.director_appraisal.director_appraisal.model.administrative.Hackathons;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HackathonsRepository extends JpaRepository<Hackathons, Long> {
    List<Hackathons> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
