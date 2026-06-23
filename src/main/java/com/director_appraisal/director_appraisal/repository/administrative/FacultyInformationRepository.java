package com.director_appraisal.director_appraisal.repository.administrative;

import com.director_appraisal.director_appraisal.model.administrative.FacultyInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FacultyInformationRepository extends JpaRepository<FacultyInformation, Long> {
    List<FacultyInformation> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
