package com.director_appraisal.director_appraisal.controller.administrative;

import com.director_appraisal.director_appraisal.model.administrative.TrainingActivities;
import com.director_appraisal.director_appraisal.service.administrative.TrainingActivitiesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/training_activities")
@RequiredArgsConstructor
@CrossOrigin
public class TrainingActivitiesController {

    private final TrainingActivitiesService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<TrainingActivities>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<TrainingActivities>> save(@PathVariable Long submissionId, @RequestBody List<TrainingActivities> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
