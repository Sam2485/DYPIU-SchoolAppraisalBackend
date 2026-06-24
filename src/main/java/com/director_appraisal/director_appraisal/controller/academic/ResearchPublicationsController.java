package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.ResearchPublications;
import com.director_appraisal.director_appraisal.service.academic.ResearchPublicationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/research_publications")
@RequiredArgsConstructor
@CrossOrigin
public class ResearchPublicationsController {

    private final ResearchPublicationsService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<ResearchPublications>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<ResearchPublications>> save(@PathVariable Long submissionId, @RequestBody List<ResearchPublications> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<ResearchPublications>> update(@PathVariable Long submissionId, @RequestBody List<ResearchPublications> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
