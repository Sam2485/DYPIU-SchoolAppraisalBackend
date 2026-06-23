package com.director_appraisal.director_appraisal.controller.administrative;

import com.director_appraisal.director_appraisal.model.administrative.ScholarshipStudents;
import com.director_appraisal.director_appraisal.service.administrative.ScholarshipStudentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/scholarship_students")
@RequiredArgsConstructor
@CrossOrigin
public class ScholarshipStudentsController {

    private final ScholarshipStudentsService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<ScholarshipStudents>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<ScholarshipStudents>> save(@PathVariable Long submissionId, @RequestBody List<ScholarshipStudents> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
