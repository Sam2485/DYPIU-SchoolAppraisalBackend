package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.StudentPlacements;
import com.director_appraisal.director_appraisal.service.academic.StudentPlacementsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/student_placements")
@RequiredArgsConstructor
@CrossOrigin
public class StudentPlacementsController {

    private final StudentPlacementsService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<StudentPlacements>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<StudentPlacements>> save(@PathVariable Long submissionId, @RequestBody List<StudentPlacements> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
