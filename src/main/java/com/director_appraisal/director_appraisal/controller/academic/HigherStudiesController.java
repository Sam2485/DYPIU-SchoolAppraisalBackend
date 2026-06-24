package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.HigherStudies;
import com.director_appraisal.director_appraisal.service.academic.HigherStudiesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/higher_studies")
@RequiredArgsConstructor
@CrossOrigin
public class HigherStudiesController {

    private final HigherStudiesService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<HigherStudies>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<HigherStudies>> save(@PathVariable Long submissionId, @RequestBody List<HigherStudies> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<HigherStudies>> update(@PathVariable Long submissionId, @RequestBody List<HigherStudies> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
