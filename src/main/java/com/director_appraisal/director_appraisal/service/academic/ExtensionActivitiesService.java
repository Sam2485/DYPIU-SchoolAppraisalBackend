package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.ExtensionActivities;
import com.director_appraisal.director_appraisal.repository.academic.ExtensionActivitiesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExtensionActivitiesService {

    private final ExtensionActivitiesRepository repository;

    public List<ExtensionActivities> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<ExtensionActivities> saveAll(Long submissionId, List<ExtensionActivities> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (ExtensionActivities row : rows) {
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
