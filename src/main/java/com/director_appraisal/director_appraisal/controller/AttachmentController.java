package com.director_appraisal.director_appraisal.controller;

import com.director_appraisal.director_appraisal.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
@CrossOrigin
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        @RequestParam(value = "section", required = false) String section) {
        try {
            enforceAdministrativeSection(section);
            AttachmentService.AttachmentResponse response = attachmentService.uploadFile(file);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to upload file: " + e.getMessage()));
        }
    }

    @PostMapping("/upload-multiple")
    public ResponseEntity<?> uploadFiles(
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "file", required = false) MultipartFile[] fallbackFiles,
            @RequestParam(value = "section", required = false) String section) {
        try {
            enforceAdministrativeSection(section);
            MultipartFile[] uploadFiles = files != null && files.length > 0 ? files : fallbackFiles;
            return ResponseEntity.ok(attachmentService.uploadFiles(uploadFiles));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to upload files: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteFile(
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "section", required = false) String section,
            @RequestBody(required = false) Map<String, String> request) {
        try {
            enforceAdministrativeSection(section != null ? section : request != null ? request.get("section") : null);
            String fileUrl = url != null && !url.isBlank()
                    ? url
                    : request != null ? request.get("url") : null;
            boolean deleted = attachmentService.deleteFile(fileUrl);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "File not found."));
            }
            return ResponseEntity.ok(Map.of("message", "File deleted successfully."));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to delete file: " + e.getMessage()));
        }
    }

    private void enforceAdministrativeSection(String section) {
        if (section == null || section.isBlank()) {
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication != null ? authentication.getPrincipal() : null;
        if (!(principal instanceof com.director_appraisal.director_appraisal.model.User user)
                || !"administrative".equalsIgnoreCase(user.getRole())) {
            return;
        }
        String post = canonicalAdministrativePost(user.getPost());
        String normalizedSection = section.trim().toUpperCase();
        boolean allowed = switch (post) {
            case "registrar" -> normalizedSection.equals("A") || normalizedSection.equals("C")
                    || normalizedSection.equals("PART_A") || normalizedSection.equals("PART_C")
                    || normalizedSection.equals("PART-A") || normalizedSection.equals("PART-C");
            case "hr" -> normalizedSection.equals("B") || normalizedSection.equals("PART_B") || normalizedSection.equals("PART-B");
            case "dean-student-welfare" -> normalizedSection.equals("D") || normalizedSection.equals("PART_D") || normalizedSection.equals("PART-D");
            case "dean-placement" -> normalizedSection.equals("E") || normalizedSection.equals("PART_E") || normalizedSection.equals("PART-E");
            default -> false;
        };
        if (!allowed) {
            throw new SecurityException("You are not authorized to modify attachments for this section");
        }
    }

    private String canonicalAdministrativePost(String post) {
        if (post == null || post.isBlank()) {
            return "";
        }
        String normalized = post.trim().toLowerCase().replace("_", "-").replaceAll("\\s+", "-");
        return switch (normalized) {
            case "registrar" -> "registrar";
            case "hr", "human-resources", "human-resource" -> "hr";
            case "dsw", "student-welfare", "dean-student-welfare", "dean-of-student-welfare" -> "dean-student-welfare";
            case "dean-placement", "placement", "dean-of-placement" -> "dean-placement";
            default -> normalized;
        };
    }
}
