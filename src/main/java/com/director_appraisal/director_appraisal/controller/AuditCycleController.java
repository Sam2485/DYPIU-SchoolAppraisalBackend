package com.director_appraisal.director_appraisal.controller;

import com.director_appraisal.director_appraisal.model.User;
import com.director_appraisal.director_appraisal.service.AcademicYearService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/audit-cycles")
@RequiredArgsConstructor
@CrossOrigin
public class AuditCycleController {

    private final AcademicYearService academicYearService;

    @PostMapping("/start-next")
    @PreAuthorize("hasAnyRole('ROLE_VICE-CHANCELLOR', 'ROLE_IQAC')")
    public ResponseEntity<Map<String, Object>> startNextAcademicYear(@RequestBody StartNextAcademicYearRequest request) {
        return ResponseEntity.ok(academicYearService.startNextAcademicYear(
                request.getCurrentAcademicYear(),
                request.getNextAcademicYear(),
                request.isPreserveApprovedHistory(),
                request.isResetActiveForms(),
                getCurrentUserDetails()
        ));
    }

    private User getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new IllegalStateException("User not authenticated properly");
    }

    @Data
    public static class StartNextAcademicYearRequest {
        private String currentAcademicYear;
        private String nextAcademicYear;
        private boolean preserveApprovedHistory = true;
        private boolean resetActiveForms = true;
    }
}
