package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.StudentStartups;
import com.director_appraisal.director_appraisal.service.academic.StudentStartupsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/student_startups")
@RequiredArgsConstructor
@CrossOrigin
public class StudentStartupsController {

    private final StudentStartupsService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<StudentStartups>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<StudentStartups>> save(@PathVariable Long submissionId, @RequestBody List<StudentStartups> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
