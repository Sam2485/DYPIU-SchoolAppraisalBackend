package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.ObeImplementation;
import com.director_appraisal.director_appraisal.repository.academic.ObeImplementationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ObeImplementationService {

    private final ObeImplementationRepository repository;

    public List<ObeImplementation> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<ObeImplementation> saveAll(Long submissionId, List<ObeImplementation> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (ObeImplementation row : rows) {
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
