package com.director_appraisal.director_appraisal.service.academic;

import com.director_appraisal.director_appraisal.model.academic.GraduatingStudents;
import com.director_appraisal.director_appraisal.repository.academic.GraduatingStudentsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GraduatingStudentsService {

    private final GraduatingStudentsRepository repository;

    public List<GraduatingStudents> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<GraduatingStudents> saveAll(Long submissionId, List<GraduatingStudents> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (GraduatingStudents row : rows) {
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
