package com.director_appraisal.director_appraisal.service.administrative;

import com.director_appraisal.director_appraisal.model.administrative.FacultyTenure;
import com.director_appraisal.director_appraisal.repository.administrative.FacultyTenureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FacultyTenureService {

    private final FacultyTenureRepository repository;

    public List<FacultyTenure> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<FacultyTenure> saveAll(Long submissionId, List<FacultyTenure> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (FacultyTenure row : rows) {
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
