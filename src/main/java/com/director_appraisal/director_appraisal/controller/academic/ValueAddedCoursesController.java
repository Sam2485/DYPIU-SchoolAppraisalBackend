package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.ValueAddedCourses;
import com.director_appraisal.director_appraisal.service.academic.ValueAddedCoursesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/value_added_courses")
@RequiredArgsConstructor
@CrossOrigin
public class ValueAddedCoursesController {

    private final ValueAddedCoursesService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<ValueAddedCourses>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<ValueAddedCourses>> save(@PathVariable Long submissionId, @RequestBody List<ValueAddedCourses> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<ValueAddedCourses>> update(@PathVariable Long submissionId, @RequestBody List<ValueAddedCourses> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
