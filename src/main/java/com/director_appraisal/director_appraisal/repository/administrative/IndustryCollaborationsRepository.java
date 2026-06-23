package com.director_appraisal.director_appraisal.repository.administrative;

import com.director_appraisal.director_appraisal.model.administrative.IndustryCollaborations;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IndustryCollaborationsRepository extends JpaRepository<IndustryCollaborations, Long> {
    List<IndustryCollaborations> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
