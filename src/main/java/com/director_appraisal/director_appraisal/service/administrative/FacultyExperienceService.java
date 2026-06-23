package com.director_appraisal.director_appraisal.service.administrative;

import com.director_appraisal.director_appraisal.model.administrative.FacultyExperience;
import com.director_appraisal.director_appraisal.repository.administrative.FacultyExperienceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FacultyExperienceService {

    private final FacultyExperienceRepository repository;

    public List<FacultyExperience> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<FacultyExperience> saveAll(Long submissionId, List<FacultyExperience> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (FacultyExperience row : rows) {
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
