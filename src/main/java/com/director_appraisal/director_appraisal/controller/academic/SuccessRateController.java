package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.SuccessRate;
import com.director_appraisal.director_appraisal.service.academic.SuccessRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/success_rate")
@RequiredArgsConstructor
@CrossOrigin
public class SuccessRateController {

    private final SuccessRateService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<SuccessRate>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<SuccessRate>> save(@PathVariable Long submissionId, @RequestBody List<SuccessRate> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
