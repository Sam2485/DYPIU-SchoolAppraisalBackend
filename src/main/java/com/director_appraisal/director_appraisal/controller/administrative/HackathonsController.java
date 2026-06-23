package com.director_appraisal.director_appraisal.controller.administrative;

import com.director_appraisal.director_appraisal.model.administrative.Hackathons;
import com.director_appraisal.director_appraisal.service.administrative.HackathonsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/hackathons")
@RequiredArgsConstructor
@CrossOrigin
public class HackathonsController {

    private final HackathonsService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<Hackathons>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<Hackathons>> save(@PathVariable Long submissionId, @RequestBody List<Hackathons> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
