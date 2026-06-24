package com.director_appraisal.director_appraisal.controller.administrative;

import com.director_appraisal.director_appraisal.model.administrative.BuildingInfrastructure;
import com.director_appraisal.director_appraisal.service.administrative.BuildingInfrastructureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/building_infrastructure")
@RequiredArgsConstructor
@CrossOrigin
public class BuildingInfrastructureController {

    private final BuildingInfrastructureService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<BuildingInfrastructure>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<BuildingInfrastructure>> save(@PathVariable Long submissionId, @RequestBody List<BuildingInfrastructure> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<BuildingInfrastructure>> update(@PathVariable Long submissionId, @RequestBody List<BuildingInfrastructure> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
