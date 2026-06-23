package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.FacultySpecialization;
import com.director_appraisal.director_appraisal.service.academic.FacultySpecializationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/faculty_specialization")
@RequiredArgsConstructor
@CrossOrigin
public class FacultySpecializationController {

    private final FacultySpecializationService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<FacultySpecialization>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<FacultySpecialization>> save(@PathVariable Long submissionId, @RequestBody List<FacultySpecialization> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
