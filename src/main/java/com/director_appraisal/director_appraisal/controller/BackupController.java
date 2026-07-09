package com.director_appraisal.director_appraisal.controller;

import com.director_appraisal.director_appraisal.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
@CrossOrigin
@PreAuthorize("hasRole('IQAC')")
public class BackupController {

    private final BackupService backupService;

    @GetMapping("/db")
    public ResponseEntity<?> downloadDbDump() {
        try {
            byte[] sqlData = backupService.createDatabaseBackup();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=db_dump_" + System.currentTimeMillis() + ".sql")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(sqlData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Database export failed: " + e.getMessage()));
        }
    }

    @PostMapping("/db/restore")
    public ResponseEntity<?> restoreDbDump(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || !Objects.requireNonNull(file.getOriginalFilename()).endsWith(".sql")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Only SQL dump files (.sql) are allowed."));
        }
        try {
            backupService.restoreDatabaseBackup(file);
            return ResponseEntity.ok(Map.of("message", "Database successfully restored from dump file."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Database restore failed: " + e.getMessage()));
        }
    }

    @GetMapping("/uploads")
    public ResponseEntity<?> downloadUploadsZip() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            backupService.createUploadsBackup(baos);
            byte[] zipData = baos.toByteArray();
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=uploads_backup_" + System.currentTimeMillis() + ".zip")
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .body(zipData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Files export failed: " + e.getMessage()));
        }
    }

    @PostMapping("/uploads/restore")
    public ResponseEntity<?> restoreUploadsZip(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || !Objects.requireNonNull(file.getOriginalFilename()).endsWith(".zip")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Only ZIP backup archives (.zip) are allowed."));
        }
        try {
            backupService.restoreUploadsBackup(file);
            return ResponseEntity.ok(Map.of("message", "Upload attachments successfully restored."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Files restore failed: " + e.getMessage()));
        }
    }
}
