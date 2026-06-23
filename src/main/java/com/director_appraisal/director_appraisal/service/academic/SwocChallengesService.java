package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.SwocChallenges;
import com.director_appraisal.director_appraisal.repository.academic.SwocChallengesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SwocChallengesService {

    private final SwocChallengesRepository repository;

    public List<SwocChallenges> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<SwocChallenges> saveAll(Long submissionId, List<SwocChallenges> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (SwocChallenges row : rows) {
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
