package com.director_appraisal.director_appraisal.service.administrative;

import com.director_appraisal.director_appraisal.model.administrative.StatutoryBodies;
import com.director_appraisal.director_appraisal.repository.administrative.StatutoryBodiesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatutoryBodiesService {

    private final StatutoryBodiesRepository repository;

    public List<StatutoryBodies> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<StatutoryBodies> saveAll(Long submissionId, List<StatutoryBodies> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (StatutoryBodies row : rows) {
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
