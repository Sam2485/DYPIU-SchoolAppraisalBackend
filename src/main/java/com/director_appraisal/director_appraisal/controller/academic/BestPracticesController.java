package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.BestPractices;
import com.director_appraisal.director_appraisal.service.academic.BestPracticesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/best_practices")
@RequiredArgsConstructor
@CrossOrigin
public class BestPracticesController {

    private final BestPracticesService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<BestPractices>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<BestPractices>> save(@PathVariable Long submissionId, @RequestBody List<BestPractices> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<BestPractices>> update(@PathVariable Long submissionId, @RequestBody List<BestPractices> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
