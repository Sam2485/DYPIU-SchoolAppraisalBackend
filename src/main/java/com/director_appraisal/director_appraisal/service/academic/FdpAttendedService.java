package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.FdpAttended;
import com.director_appraisal.director_appraisal.repository.academic.FdpAttendedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FdpAttendedService {

    private final FdpAttendedRepository repository;

    public List<FdpAttended> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<FdpAttended> saveAll(Long submissionId, List<FdpAttended> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (FdpAttended row : rows) {
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
