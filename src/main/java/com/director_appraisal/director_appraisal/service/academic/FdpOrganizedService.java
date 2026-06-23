package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.FdpOrganized;
import com.director_appraisal.director_appraisal.repository.academic.FdpOrganizedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FdpOrganizedService {

    private final FdpOrganizedRepository repository;

    public List<FdpOrganized> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<FdpOrganized> saveAll(Long submissionId, List<FdpOrganized> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (FdpOrganized row : rows) {
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
