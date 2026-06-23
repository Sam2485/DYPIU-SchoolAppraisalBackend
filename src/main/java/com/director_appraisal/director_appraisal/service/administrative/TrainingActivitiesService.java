package com.director_appraisal.director_appraisal.service.administrative;

import com.director_appraisal.director_appraisal.model.administrative.TrainingActivities;
import com.director_appraisal.director_appraisal.repository.administrative.TrainingActivitiesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainingActivitiesService {

    private final TrainingActivitiesRepository repository;

    public List<TrainingActivities> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<TrainingActivities> saveAll(Long submissionId, List<TrainingActivities> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (TrainingActivities row : rows) {
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
