package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.SwocStrength;
import com.director_appraisal.director_appraisal.service.academic.SwocStrengthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/swoc_strength")
@RequiredArgsConstructor
@CrossOrigin
public class SwocStrengthController {

    private final SwocStrengthService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<SwocStrength>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<SwocStrength>> save(@PathVariable Long submissionId, @RequestBody List<SwocStrength> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
