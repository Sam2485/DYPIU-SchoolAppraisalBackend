package com.director_appraisal.director_appraisal.controller.administrative;

import com.director_appraisal.director_appraisal.model.administrative.StaffTraining;
import com.director_appraisal.director_appraisal.service.administrative.StaffTrainingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/staff_training")
@RequiredArgsConstructor
@CrossOrigin
public class StaffTrainingController {

    private final StaffTrainingService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<StaffTraining>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<StaffTraining>> save(@PathVariable Long submissionId, @RequestBody List<StaffTraining> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
