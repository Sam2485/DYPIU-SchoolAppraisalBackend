package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.StudentAwards;
import com.director_appraisal.director_appraisal.service.academic.StudentAwardsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/student_awards")
@RequiredArgsConstructor
@CrossOrigin
public class StudentAwardsController {

    private final StudentAwardsService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<StudentAwards>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<StudentAwards>> save(@PathVariable Long submissionId, @RequestBody List<StudentAwards> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
