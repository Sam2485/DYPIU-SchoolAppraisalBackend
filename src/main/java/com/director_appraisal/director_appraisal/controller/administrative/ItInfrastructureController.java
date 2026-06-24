package com.director_appraisal.director_appraisal.controller.administrative;

import com.director_appraisal.director_appraisal.model.administrative.ItInfrastructure;
import com.director_appraisal.director_appraisal.service.administrative.ItInfrastructureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/it_infrastructure")
@RequiredArgsConstructor
@CrossOrigin
public class ItInfrastructureController {

    private final ItInfrastructureService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<ItInfrastructure>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<ItInfrastructure>> save(@PathVariable Long submissionId, @RequestBody List<ItInfrastructure> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<ItInfrastructure>> update(@PathVariable Long submissionId, @RequestBody List<ItInfrastructure> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
