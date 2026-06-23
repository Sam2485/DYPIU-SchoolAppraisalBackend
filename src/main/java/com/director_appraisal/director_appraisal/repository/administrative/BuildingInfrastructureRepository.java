package com.director_appraisal.director_appraisal.repository.administrative;

import com.director_appraisal.director_appraisal.model.administrative.BuildingInfrastructure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BuildingInfrastructureRepository extends JpaRepository<BuildingInfrastructure, Long> {
    List<BuildingInfrastructure> findBySubmissionId(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
