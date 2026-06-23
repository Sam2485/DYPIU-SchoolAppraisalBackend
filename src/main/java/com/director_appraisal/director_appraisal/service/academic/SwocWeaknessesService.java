package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.SwocWeaknesses;
import com.director_appraisal.director_appraisal.repository.academic.SwocWeaknessesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SwocWeaknessesService {

    private final SwocWeaknessesRepository repository;

    public List<SwocWeaknesses> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<SwocWeaknesses> saveAll(Long submissionId, List<SwocWeaknesses> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (SwocWeaknesses row : rows) {
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
