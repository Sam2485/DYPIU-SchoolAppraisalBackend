package com.director_appraisal.director_appraisal.controller.administrative;

import com.director_appraisal.director_appraisal.model.administrative.CulturalActivities;
import com.director_appraisal.director_appraisal.service.administrative.CulturalActivitiesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/cultural_activities")
@RequiredArgsConstructor
@CrossOrigin
public class CulturalActivitiesController {

    private final CulturalActivitiesService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<CulturalActivities>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<CulturalActivities>> save(@PathVariable Long submissionId, @RequestBody List<CulturalActivities> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<CulturalActivities>> update(@PathVariable Long submissionId, @RequestBody List<CulturalActivities> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
