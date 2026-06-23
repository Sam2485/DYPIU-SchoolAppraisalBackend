package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.SwocOpportunities;
import com.director_appraisal.director_appraisal.repository.academic.SwocOpportunitiesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SwocOpportunitiesService {

    private final SwocOpportunitiesRepository repository;

    public List<SwocOpportunities> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<SwocOpportunities> saveAll(Long submissionId, List<SwocOpportunities> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (SwocOpportunities row : rows) {
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
