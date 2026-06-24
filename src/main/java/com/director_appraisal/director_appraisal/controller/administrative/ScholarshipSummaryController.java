package com.director_appraisal.director_appraisal.controller.administrative;

import com.director_appraisal.director_appraisal.model.administrative.ScholarshipSummary;
import com.director_appraisal.director_appraisal.service.administrative.ScholarshipSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/scholarship_summary")
@RequiredArgsConstructor
@CrossOrigin
public class ScholarshipSummaryController {

    private final ScholarshipSummaryService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<ScholarshipSummary>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<ScholarshipSummary>> save(@PathVariable Long submissionId, @RequestBody List<ScholarshipSummary> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<ScholarshipSummary>> update(@PathVariable Long submissionId, @RequestBody List<ScholarshipSummary> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
