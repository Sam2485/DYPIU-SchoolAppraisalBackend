package com.director_appraisal.director_appraisal.controller.administrative;

import com.director_appraisal.director_appraisal.model.administrative.LibraryInfrastructure;
import com.director_appraisal.director_appraisal.service.administrative.LibraryInfrastructureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/library_infrastructure")
@RequiredArgsConstructor
@CrossOrigin
public class LibraryInfrastructureController {

    private final LibraryInfrastructureService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<LibraryInfrastructure>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<LibraryInfrastructure>> save(@PathVariable Long submissionId, @RequestBody List<LibraryInfrastructure> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
