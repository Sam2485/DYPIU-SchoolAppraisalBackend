package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.FdpAttended;
import com.director_appraisal.director_appraisal.service.academic.FdpAttendedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/fdp_attended")
@RequiredArgsConstructor
@CrossOrigin
public class FdpAttendedController {

    private final FdpAttendedService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<FdpAttended>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<FdpAttended>> save(@PathVariable Long submissionId, @RequestBody List<FdpAttended> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
