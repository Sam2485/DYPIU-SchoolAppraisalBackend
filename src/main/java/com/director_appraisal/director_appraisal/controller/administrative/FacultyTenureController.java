package com.director_appraisal.director_appraisal.controller.administrative;

import com.director_appraisal.director_appraisal.model.administrative.FacultyTenure;
import com.director_appraisal.director_appraisal.service.administrative.FacultyTenureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/faculty_tenure")
@RequiredArgsConstructor
@CrossOrigin
public class FacultyTenureController {

    private final FacultyTenureService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<FacultyTenure>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<FacultyTenure>> save(@PathVariable Long submissionId, @RequestBody List<FacultyTenure> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<FacultyTenure>> update(@PathVariable Long submissionId, @RequestBody List<FacultyTenure> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
