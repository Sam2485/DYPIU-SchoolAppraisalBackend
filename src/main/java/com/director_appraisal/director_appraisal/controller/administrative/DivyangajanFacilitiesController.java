package com.director_appraisal.director_appraisal.controller.administrative;

import com.director_appraisal.director_appraisal.model.administrative.DivyangajanFacilities;
import com.director_appraisal.director_appraisal.service.administrative.DivyangajanFacilitiesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/divyangajan_facilities")
@RequiredArgsConstructor
@CrossOrigin
public class DivyangajanFacilitiesController {

    private final DivyangajanFacilitiesService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<DivyangajanFacilities>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<DivyangajanFacilities>> save(@PathVariable Long submissionId, @RequestBody List<DivyangajanFacilities> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @PutMapping("/submission/{submissionId}")
    public ResponseEntity<List<DivyangajanFacilities>> update(@PathVariable Long submissionId, @RequestBody List<DivyangajanFacilities> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
