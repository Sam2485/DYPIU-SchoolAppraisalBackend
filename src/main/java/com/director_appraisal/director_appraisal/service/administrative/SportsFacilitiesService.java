package com.director_appraisal.director_appraisal.service.administrative;

import com.director_appraisal.director_appraisal.model.administrative.SportsFacilities;
import com.director_appraisal.director_appraisal.repository.administrative.SportsFacilitiesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SportsFacilitiesService {

    private final SportsFacilitiesRepository repository;

    public List<SportsFacilities> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<SportsFacilities> saveAll(Long submissionId, List<SportsFacilities> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (SportsFacilities row : rows) {
            row.setId(null);
            row.setSubmissionId(submissionId);
        }
        return repository.saveAll(rows);
    }

    @Transactional
    public void deleteBySubmissionId(Long submissionId) {
        repository.deleteBySubmissionId(submissionId);
    }
}
