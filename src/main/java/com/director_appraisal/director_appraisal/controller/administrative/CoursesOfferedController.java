package com.director_appraisal.director_appraisal.controller.administrative;

import com.director_appraisal.director_appraisal.model.administrative.CoursesOffered;
import com.director_appraisal.director_appraisal.service.administrative.CoursesOfferedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/courses_offered")
@RequiredArgsConstructor
@CrossOrigin
public class CoursesOfferedController {

    private final CoursesOfferedService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<CoursesOffered>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<CoursesOffered>> save(@PathVariable Long submissionId, @RequestBody List<CoursesOffered> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<CoursesOffered>> update(@PathVariable Long submissionId, @RequestBody List<CoursesOffered> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
