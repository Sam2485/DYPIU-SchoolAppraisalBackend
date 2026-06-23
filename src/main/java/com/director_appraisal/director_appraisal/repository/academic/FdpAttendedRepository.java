package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.FdpAttended;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FdpAttendedRepository extends JpaRepository<FdpAttended, Long> {
    List<FdpAttended> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
