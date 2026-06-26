package com.director_appraisal.director_appraisal.repository;

import com.director_appraisal.director_appraisal.model.Snapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SnapshotRepository extends JpaRepository<Snapshot, Long> {
    List<Snapshot> findBySubmissionIdOrderByVersionDesc(Long submissionId);
    void deleteBySubmissionId(Long submissionId);
}
