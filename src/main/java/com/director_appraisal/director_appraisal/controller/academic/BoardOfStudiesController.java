package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.BoardOfStudies;
import com.director_appraisal.director_appraisal.service.academic.BoardOfStudiesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/board_of_studies")
@RequiredArgsConstructor
@CrossOrigin
public class BoardOfStudiesController {

    private final BoardOfStudiesService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<BoardOfStudies>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<BoardOfStudies>> save(@PathVariable Long submissionId, @RequestBody List<BoardOfStudies> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
