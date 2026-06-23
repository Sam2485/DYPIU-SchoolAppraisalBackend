package com.director_appraisal.director_appraisal.controller.administrative;

import com.director_appraisal.director_appraisal.model.administrative.EResources;
import com.director_appraisal.director_appraisal.service.administrative.EResourcesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/e_resources")
@RequiredArgsConstructor
@CrossOrigin
public class EResourcesController {

    private final EResourcesService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<EResources>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<EResources>> save(@PathVariable Long submissionId, @RequestBody List<EResources> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
