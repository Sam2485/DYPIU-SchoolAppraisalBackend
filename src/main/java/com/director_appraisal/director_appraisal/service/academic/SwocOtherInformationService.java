package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.SwocOtherInformation;
import com.director_appraisal.director_appraisal.repository.academic.SwocOtherInformationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SwocOtherInformationService {

    private final SwocOtherInformationRepository repository;

    public List<SwocOtherInformation> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<SwocOtherInformation> saveAll(Long submissionId, List<SwocOtherInformation> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (SwocOtherInformation row : rows) {
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
