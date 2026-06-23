package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.FacultyStrength;
import com.director_appraisal.director_appraisal.service.academic.FacultyStrengthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/faculty_strength")
@RequiredArgsConstructor
@CrossOrigin
public class FacultyStrengthController {

    private final FacultyStrengthService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<FacultyStrength>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<FacultyStrength>> save(@PathVariable Long submissionId, @RequestBody List<FacultyStrength> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
