package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.StudentCourses;
import com.director_appraisal.director_appraisal.service.academic.StudentCoursesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/student_courses")
@RequiredArgsConstructor
@CrossOrigin
public class StudentCoursesController {

    private final StudentCoursesService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<StudentCourses>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<StudentCourses>> save(@PathVariable Long submissionId, @RequestBody List<StudentCourses> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<StudentCourses>> update(@PathVariable Long submissionId, @RequestBody List<StudentCourses> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
