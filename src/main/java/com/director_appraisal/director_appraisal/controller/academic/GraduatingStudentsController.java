package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.GraduatingStudents;
import com.director_appraisal.director_appraisal.service.academic.GraduatingStudentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/graduating_students")
@RequiredArgsConstructor
@CrossOrigin
public class GraduatingStudentsController {

    private final GraduatingStudentsService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<GraduatingStudents>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<GraduatingStudents>> save(@PathVariable Long submissionId, @RequestBody List<GraduatingStudents> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<GraduatingStudents>> update(@PathVariable Long submissionId, @RequestBody List<GraduatingStudents> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
