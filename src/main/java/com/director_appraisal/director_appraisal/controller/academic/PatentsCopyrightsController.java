package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.PatentsCopyrights;
import com.director_appraisal.director_appraisal.service.academic.PatentsCopyrightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/patents_copyrights")
@RequiredArgsConstructor
@CrossOrigin
public class PatentsCopyrightsController {

    private final PatentsCopyrightsService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<PatentsCopyrights>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<PatentsCopyrights>> save(@PathVariable Long submissionId, @RequestBody List<PatentsCopyrights> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<PatentsCopyrights>> update(@PathVariable Long submissionId, @RequestBody List<PatentsCopyrights> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
