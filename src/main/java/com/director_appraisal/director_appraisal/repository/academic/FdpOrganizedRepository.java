package com.director_appraisal.director_appraisal.repository.academic;

import com.director_appraisal.director_appraisal.model.academic.FdpOrganized;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FdpOrganizedRepository extends JpaRepository<FdpOrganized, Long> {
    List<FdpOrganized> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
