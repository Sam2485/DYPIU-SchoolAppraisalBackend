package com.director_appraisal.director_appraisal.service.administrative;

import com.director_appraisal.director_appraisal.model.administrative.DivyangajanFacilities;
import com.director_appraisal.director_appraisal.repository.administrative.DivyangajanFacilitiesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DivyangajanFacilitiesService {

    private final DivyangajanFacilitiesRepository repository;

    public List<DivyangajanFacilities> getBySubmissionId(Long submissionId) {
        return repository.findBySubmissionId(submissionId);
    }

    @Transactional
    public List<DivyangajanFacilities> saveAll(Long submissionId, List<DivyangajanFacilities> rows) {
        repository.deleteBySubmissionId(submissionId);
        for (DivyangajanFacilities row : rows) {
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
