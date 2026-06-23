package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.NepStatus;
import com.director_appraisal.director_appraisal.service.academic.NepStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/nep_status")
@RequiredArgsConstructor
@CrossOrigin
public class NepStatusController {

    private final NepStatusService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<NepStatus>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<NepStatus>> save(@PathVariable Long submissionId, @RequestBody List<NepStatus> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
