package com.director_appraisal.director_appraisal.controller.administrative;

import com.director_appraisal.director_appraisal.model.administrative.SportsActivities;
import com.director_appraisal.director_appraisal.service.administrative.SportsActivitiesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/sports_activities")
@RequiredArgsConstructor
@CrossOrigin
public class SportsActivitiesController {

    private final SportsActivitiesService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<SportsActivities>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<SportsActivities>> save(@PathVariable Long submissionId, @RequestBody List<SportsActivities> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<SportsActivities>> update(@PathVariable Long submissionId, @RequestBody List<SportsActivities> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
