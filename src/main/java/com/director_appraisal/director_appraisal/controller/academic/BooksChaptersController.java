package com.director_appraisal.director_appraisal.controller.academic;

import com.director_appraisal.director_appraisal.model.academic.BooksChapters;
import com.director_appraisal.director_appraisal.service.academic.BooksChaptersService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables/books_chapters")
@RequiredArgsConstructor
@CrossOrigin
public class BooksChaptersController {

    private final BooksChaptersService service;

    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<List<BooksChapters>> getBySubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(service.getBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    public ResponseEntity<List<BooksChapters>> save(@PathVariable Long submissionId, @RequestBody List<BooksChapters> rows) {
        return ResponseEntity.ok(service.saveAll(submissionId, rows));
    }

    @DeleteMapping("/submission/{submissionId}")
    public ResponseEntity<Void> delete(@PathVariable Long submissionId) {
        service.deleteBySubmissionId(submissionId);
        return ResponseEntity.ok().build();
    }
}
