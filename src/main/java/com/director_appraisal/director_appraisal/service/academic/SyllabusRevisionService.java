package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.SyllabusRevision;
import com.director_appraisal.director_appraisal.repository.academic.SyllabusRevisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SyllabusRevisionService {

    private final SyllabusRevisionRepository repository;

    public List<SyllabusRevision> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<SyllabusRevision> saveAll(Long submissionId, List<SyllabusRevision> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (SyllabusRevision row : rows) {
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
