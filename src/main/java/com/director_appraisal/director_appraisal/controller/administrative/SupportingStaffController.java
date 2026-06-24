package com.director_appraisal.director_appraisal.controller.administrative;

import com.director_appraisal.director_appraisal.model.administrative.SupportingStaff;
import com.director_appraisal.director_appraisal.service.administrative.SupportingStaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/supporting_staff")
@RequiredArgsConstructor
@CrossOrigin
public class SupportingStaffController {

    private final SupportingStaffService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<SupportingStaff>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<SupportingStaff>> save(@PathVariable Long submissionId, @RequestBody List<SupportingStaff> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<SupportingStaff>> update(@PathVariable Long submissionId, @RequestBody List<SupportingStaff> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
