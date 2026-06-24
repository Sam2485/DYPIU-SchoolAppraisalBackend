package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.SwocOpportunities;
import com.director_appraisal.director_appraisal.service.academic.SwocOpportunitiesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/swoc_opportunities")
@RequiredArgsConstructor
@CrossOrigin
public class SwocOpportunitiesController {

    private final SwocOpportunitiesService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<SwocOpportunities>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<SwocOpportunities>> save(@PathVariable Long submissionId, @RequestBody List<SwocOpportunities> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<SwocOpportunities>> update(@PathVariable Long submissionId, @RequestBody List<SwocOpportunities> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
