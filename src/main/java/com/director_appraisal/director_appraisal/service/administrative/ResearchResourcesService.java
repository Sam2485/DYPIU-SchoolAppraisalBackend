package com.director_appraisal.director_appraisal.service.administrative;

import com.director_appraisal.director_appraisal.model.administrative.ResearchResources;
import com.director_appraisal.director_appraisal.repository.administrative.ResearchResourcesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResearchResourcesService {

    private final ResearchResourcesRepository repository;

    public List<ResearchResources> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<ResearchResources> saveAll(Long submissionId, List<ResearchResources> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (ResearchResources row : rows) {
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
