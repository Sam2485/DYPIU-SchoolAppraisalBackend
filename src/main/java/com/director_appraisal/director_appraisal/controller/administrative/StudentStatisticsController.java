package com.director_appraisal.director_appraisal.controller.administrative;

import com.director_appraisal.director_appraisal.model.administrative.StudentStatistics;
import com.director_appraisal.director_appraisal.service.administrative.StudentStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/student_statistics")
@RequiredArgsConstructor
@CrossOrigin
public class StudentStatisticsController {

    private final StudentStatisticsService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<StudentStatistics>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<StudentStatistics>> save(@PathVariable Long submissionId, @RequestBody List<StudentStatistics> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<StudentStatistics>> update(@PathVariable Long submissionId, @RequestBody List<StudentStatistics> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
