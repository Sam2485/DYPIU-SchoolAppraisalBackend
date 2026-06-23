package com.director_appraisal.director_appraisal.service.administrative;

import com.director_appraisal.director_appraisal.model.administrative.CulturalActivities;
import com.director_appraisal.director_appraisal.repository.administrative.CulturalActivitiesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CulturalActivitiesService {

    private final CulturalActivitiesRepository repository;

    public List<CulturalActivities> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<CulturalActivities> saveAll(Long submissionId, List<CulturalActivities> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (CulturalActivities row : rows) {
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
