package com.director_appraisal.director_appraisal.service.administrative;

import com.director_appraisal.director_appraisal.model.administrative.AuditRecords;
import com.director_appraisal.director_appraisal.repository.administrative.AuditRecordsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditRecordsService {

    private final AuditRecordsRepository repository;

    public List<AuditRecords> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<AuditRecords> saveAll(Long submissionId, List<AuditRecords> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (AuditRecords row : rows) {
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
