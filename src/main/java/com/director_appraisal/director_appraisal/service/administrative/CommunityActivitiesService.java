package com.director_appraisal.director_appraisal.service.administrative;

import com.director_appraisal.director_appraisal.model.administrative.CommunityActivities;
import com.director_appraisal.director_appraisal.repository.administrative.CommunityActivitiesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityActivitiesService {

    private final CommunityActivitiesRepository repository;

    public List<CommunityActivities> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<CommunityActivities> saveAll(Long submissionId, List<CommunityActivities> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (CommunityActivities row : rows) {
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
