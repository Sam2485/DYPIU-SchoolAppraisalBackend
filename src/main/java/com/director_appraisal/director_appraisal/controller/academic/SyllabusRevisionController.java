package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.SyllabusRevision;
import com.director_appraisal.director_appraisal.service.academic.SyllabusRevisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/syllabus_revision")
@RequiredArgsConstructor
@CrossOrigin
public class SyllabusRevisionController {

    private final SyllabusRevisionService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<SyllabusRevision>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<SyllabusRevision>> save(@PathVariable Long submissionId, @RequestBody List<SyllabusRevision> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<SyllabusRevision>> update(@PathVariable Long submissionId, @RequestBody List<SyllabusRevision> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
