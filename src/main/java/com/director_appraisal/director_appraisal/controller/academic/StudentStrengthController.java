package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.StudentStrength;
import com.director_appraisal.director_appraisal.service.academic.StudentStrengthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/student_strength")
@RequiredArgsConstructor
@CrossOrigin
public class StudentStrengthController {

    private final StudentStrengthService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<StudentStrength>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<StudentStrength>> save(@PathVariable Long submissionId, @RequestBody List<StudentStrength> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
