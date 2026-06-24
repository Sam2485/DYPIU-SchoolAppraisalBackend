package com.director_appraisal.director_appraisal.controller.administrative;

import com.director_appraisal.director_appraisal.model.administrative.ResearchResources;
import com.director_appraisal.director_appraisal.service.administrative.ResearchResourcesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/research_resources")
@RequiredArgsConstructor
@CrossOrigin
public class ResearchResourcesController {

    private final ResearchResourcesService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<ResearchResources>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<ResearchResources>> save(@PathVariable Long submissionId, @RequestBody List<ResearchResources> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<ResearchResources>> update(@PathVariable Long submissionId, @RequestBody List<ResearchResources> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
