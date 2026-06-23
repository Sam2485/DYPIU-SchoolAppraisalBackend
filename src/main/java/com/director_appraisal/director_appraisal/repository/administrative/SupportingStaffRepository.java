package com.director_appraisal.director_appraisal.repository.administrative;

import com.director_appraisal.director_appraisal.model.administrative.SupportingStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SupportingStaffRepository extends JpaRepository<SupportingStaff, Long> {
    List<SupportingStaff> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
