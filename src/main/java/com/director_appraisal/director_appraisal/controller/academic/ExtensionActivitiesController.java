package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.ExtensionActivities;
import com.director_appraisal.director_appraisal.service.academic.ExtensionActivitiesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/extension_activities")
@RequiredArgsConstructor
@CrossOrigin
public class ExtensionActivitiesController {

    private final ExtensionActivitiesService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<ExtensionActivities>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<ExtensionActivities>> save(@PathVariable Long submissionId, @RequestBody List<ExtensionActivities> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<ExtensionActivities>> update(@PathVariable Long submissionId, @RequestBody List<ExtensionActivities> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
