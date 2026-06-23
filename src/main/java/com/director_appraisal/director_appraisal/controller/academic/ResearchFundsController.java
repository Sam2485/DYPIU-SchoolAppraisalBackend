package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.ResearchFunds;
import com.director_appraisal.director_appraisal.service.academic.ResearchFundsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/research_funds")
@RequiredArgsConstructor
@CrossOrigin
public class ResearchFundsController {

    private final ResearchFundsService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<ResearchFunds>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<ResearchFunds>> save(@PathVariable Long submissionId, @RequestBody List<ResearchFunds> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
