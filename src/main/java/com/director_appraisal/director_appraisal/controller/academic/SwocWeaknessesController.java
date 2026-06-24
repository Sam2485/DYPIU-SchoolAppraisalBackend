package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.SwocWeaknesses;
import com.director_appraisal.director_appraisal.service.academic.SwocWeaknessesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/swoc_weaknesses")
@RequiredArgsConstructor
@CrossOrigin
public class SwocWeaknessesController {

    private final SwocWeaknessesService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<SwocWeaknesses>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<SwocWeaknesses>> save(@PathVariable Long submissionId, @RequestBody List<SwocWeaknesses> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<SwocWeaknesses>> update(@PathVariable Long submissionId, @RequestBody List<SwocWeaknesses> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
