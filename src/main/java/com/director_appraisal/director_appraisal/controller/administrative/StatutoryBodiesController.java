package com.director_appraisal.director_appraisal.controller.administrative;

import com.director_appraisal.director_appraisal.model.administrative.StatutoryBodies;
import com.director_appraisal.director_appraisal.service.administrative.StatutoryBodiesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/statutory_bodies")
@RequiredArgsConstructor
@CrossOrigin
public class StatutoryBodiesController {

    private final StatutoryBodiesService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<StatutoryBodies>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<StatutoryBodies>> save(@PathVariable Long submissionId, @RequestBody List<StatutoryBodies> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<StatutoryBodies>> update(@PathVariable Long submissionId, @RequestBody List<StatutoryBodies> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
