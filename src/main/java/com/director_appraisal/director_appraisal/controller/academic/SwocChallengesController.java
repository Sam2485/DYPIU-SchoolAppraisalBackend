package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.SwocChallenges;
import com.director_appraisal.director_appraisal.service.academic.SwocChallengesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/swoc_challenges")
@RequiredArgsConstructor
@CrossOrigin
public class SwocChallengesController {

    private final SwocChallengesService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<SwocChallenges>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<SwocChallenges>> save(@PathVariable Long submissionId, @RequestBody List<SwocChallenges> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<SwocChallenges>> update(@PathVariable Long submissionId, @RequestBody List<SwocChallenges> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
