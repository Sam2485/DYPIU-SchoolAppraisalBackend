package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.CareerGuidance;
import com.director_appraisal.director_appraisal.service.academic.CareerGuidanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/career_guidance")
@RequiredArgsConstructor
@CrossOrigin
public class CareerGuidanceController {

    private final CareerGuidanceService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<CareerGuidance>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<CareerGuidance>> save(@PathVariable Long submissionId, @RequestBody List<CareerGuidance> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
