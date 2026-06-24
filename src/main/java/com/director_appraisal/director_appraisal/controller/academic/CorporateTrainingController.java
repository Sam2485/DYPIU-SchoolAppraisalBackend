package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.CorporateTraining;
import com.director_appraisal.director_appraisal.service.academic.CorporateTrainingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/corporate_training")
@RequiredArgsConstructor
@CrossOrigin
public class CorporateTrainingController {

    private final CorporateTrainingService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<CorporateTraining>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<CorporateTraining>> save(@PathVariable Long submissionId, @RequestBody List<CorporateTraining> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<CorporateTraining>> update(@PathVariable Long submissionId, @RequestBody List<CorporateTraining> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
