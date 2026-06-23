package com.director_appraisal.director_appraisal.controller.administrative;

import com.director_appraisal.director_appraisal.model.administrative.IndustryCollaborations;
import com.director_appraisal.director_appraisal.service.administrative.IndustryCollaborationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/industry_collaborations")
@RequiredArgsConstructor
@CrossOrigin
public class IndustryCollaborationsController {

    private final IndustryCollaborationsService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<IndustryCollaborations>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<IndustryCollaborations>> save(@PathVariable Long submissionId, @RequestBody List<IndustryCollaborations> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
