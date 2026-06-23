package com.director_appraisal.director_appraisal.service.administrative;

import com.director_appraisal.director_appraisal.model.administrative.SupportingStaff;
import com.director_appraisal.director_appraisal.repository.administrative.SupportingStaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportingStaffService {

    private final SupportingStaffRepository repository;

    public List<SupportingStaff> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<SupportingStaff> saveAll(Long submissionId, List<SupportingStaff> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (SupportingStaff row : rows) {
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
