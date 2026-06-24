package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.FdpOrganized;
import com.director_appraisal.director_appraisal.service.academic.FdpOrganizedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/fdp_organized")
@RequiredArgsConstructor
@CrossOrigin
public class FdpOrganizedController {

    private final FdpOrganizedService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<FdpOrganized>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<FdpOrganized>> save(@PathVariable Long submissionId, @RequestBody List<FdpOrganized> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<FdpOrganized>> update(@PathVariable Long submissionId, @RequestBody List<FdpOrganized> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
