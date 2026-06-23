package com.director_appraisal.director_appraisal.service.administrative;

import com.director_appraisal.director_appraisal.model.administrative.FacultyInformation;
import com.director_appraisal.director_appraisal.repository.administrative.FacultyInformationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FacultyInformationService {

    private final FacultyInformationRepository repository;

    public List<FacultyInformation> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<FacultyInformation> saveAll(Long submissionId, List<FacultyInformation> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (FacultyInformation row : rows) {
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
