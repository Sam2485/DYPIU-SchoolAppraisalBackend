package com.director_appraisal.director_appraisal.controller.administrative;

import com.director_appraisal.director_appraisal.model.administrative.FacultyInformation;
import com.director_appraisal.director_appraisal.service.administrative.FacultyInformationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/faculty_information")
@RequiredArgsConstructor
@CrossOrigin
public class FacultyInformationController {

    private final FacultyInformationService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<FacultyInformation>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<FacultyInformation>> save(@PathVariable Long submissionId, @RequestBody List<FacultyInformation> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<FacultyInformation>> update(@PathVariable Long submissionId, @RequestBody List<FacultyInformation> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
