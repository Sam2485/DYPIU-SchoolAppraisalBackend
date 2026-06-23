package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.ResearchPublications;
import com.director_appraisal.director_appraisal.repository.academic.ResearchPublicationsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResearchPublicationsService {

    private final ResearchPublicationsRepository repository;

    public List<ResearchPublications> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<ResearchPublications> saveAll(Long submissionId, List<ResearchPublications> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (ResearchPublications row : rows) {
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
