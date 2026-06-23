package com.director_appraisal.director_appraisal.controller.administrative;

import com.director_appraisal.director_appraisal.model.administrative.FacultyExperience;
import com.director_appraisal.director_appraisal.service.administrative.FacultyExperienceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/faculty_experience")
@RequiredArgsConstructor
@CrossOrigin
public class FacultyExperienceController {

    private final FacultyExperienceService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<FacultyExperience>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<FacultyExperience>> save(@PathVariable Long submissionId, @RequestBody List<FacultyExperience> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
