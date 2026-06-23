package com.director_appraisal.director_appraisal.controller.administrative;

import com.director_appraisal.director_appraisal.model.administrative.SportsFacilities;
import com.director_appraisal.director_appraisal.service.administrative.SportsFacilitiesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/sports_facilities")
@RequiredArgsConstructor
@CrossOrigin
public class SportsFacilitiesController {

    private final SportsFacilitiesService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<SportsFacilities>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<SportsFacilities>> save(@PathVariable Long submissionId, @RequestBody List<SportsFacilities> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
