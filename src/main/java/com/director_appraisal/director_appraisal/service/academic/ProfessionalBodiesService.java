package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.ProfessionalBodies;
import com.director_appraisal.director_appraisal.repository.academic.ProfessionalBodiesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfessionalBodiesService {

    private final ProfessionalBodiesRepository repository;

    public List<ProfessionalBodies> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<ProfessionalBodies> saveAll(Long submissionId, List<ProfessionalBodies> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (ProfessionalBodies row : rows) {
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
