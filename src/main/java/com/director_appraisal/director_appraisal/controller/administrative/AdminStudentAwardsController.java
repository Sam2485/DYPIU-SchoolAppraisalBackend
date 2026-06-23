package com.director_appraisal.director_appraisal.controller.administrative;

import com.director_appraisal.director_appraisal.model.administrative.AdminStudentAwards;
import com.director_appraisal.director_appraisal.service.administrative.AdminStudentAwardsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/admin_student_awards")
@RequiredArgsConstructor
@CrossOrigin
public class AdminStudentAwardsController {

    private final AdminStudentAwardsService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<AdminStudentAwards>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<AdminStudentAwards>> save(@PathVariable Long submissionId, @RequestBody List<AdminStudentAwards> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
