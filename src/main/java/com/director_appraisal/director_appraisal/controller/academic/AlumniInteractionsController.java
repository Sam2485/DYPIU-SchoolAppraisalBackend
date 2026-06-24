package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.AlumniInteractions;
import com.director_appraisal.director_appraisal.service.academic.AlumniInteractionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/alumni_interactions")
@RequiredArgsConstructor
@CrossOrigin
public class AlumniInteractionsController {

    private final AlumniInteractionsService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<AlumniInteractions>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<AlumniInteractions>> save(@PathVariable Long submissionId, @RequestBody List<AlumniInteractions> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<AlumniInteractions>> update(@PathVariable Long submissionId, @RequestBody List<AlumniInteractions> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
