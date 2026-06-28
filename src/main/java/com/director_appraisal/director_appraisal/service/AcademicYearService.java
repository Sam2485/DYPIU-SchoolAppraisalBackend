package com.director_appraisal.director_appraisal.service;

import com.director_appraisal.director_appraisal.exception.ConflictException;
import com.director_appraisal.director_appraisal.model.AcademicYear;
import com.director_appraisal.director_appraisal.model.Submission;
import com.director_appraisal.director_appraisal.model.User;
import com.director_appraisal.director_appraisal.repository.AcademicYearRepository;
import com.director_appraisal.director_appraisal.repository.SubmissionRepository;
import com.director_appraisal.director_appraisal.repository.UserRepository;
import com.director_appraisal.director_appraisal.util.SchoolUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AcademicYearService {

    private static final String DEFAULT_ACADEMIC_YEAR = "2025-2026";

    private final AcademicYearRepository academicYearRepository;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;

    public String getCurrentAcademicYearLabel() {
        return academicYearRepository.findByActiveTrue().stream()
                .findFirst()
                .map(AcademicYear::getYearLabel)
                .orElse(DEFAULT_ACADEMIC_YEAR);
    }

    @Transactional
    public Map<String, Object> startNextAcademicYear(String currentAcademicYear, String nextAcademicYear,
                                                     boolean preserveApprovedHistory, boolean resetActiveForms,
                                                     User caller) {
        validateReviewer(caller);
        String current = normalizeYear(currentAcademicYear);
        String next = normalizeYear(nextAcademicYear);
        validateNextYear(current, next);

        List<AcademicYear> activeYears = academicYearRepository.findByActiveTrue();
        if (activeYears.size() != 1) {
            throw new IllegalStateException("Exactly one active academic year is required");
        }
        AcademicYear activeYear = activeYears.get(0);
        if (!activeYear.getYearLabel().equals(current)) {
            throw new IllegalArgumentException("Current academic year does not match active academic year");
        }
        if (academicYearRepository.existsByYearLabel(next)) {
            throw new ConflictException("Academic year already exists");
        }

        activeYear.setActive(false);
        activeYear.setClosedAt(LocalDateTime.now());
        academicYearRepository.save(activeYear);

        AcademicYear newYear = AcademicYear.builder()
                .yearLabel(next)
                .active(true)
                .startedAt(LocalDateTime.now())
                .build();
        academicYearRepository.save(newYear);

        int academicFormsCreated = 0;
        int administrativeFormsCreated = 0;

        for (User user : userRepository.findAll()) {
            String role = normalize(user.getRole());
            if ("director".equals(role)) {
                String school = SchoolUtils.canonicalizeSchool(user.getSchool());
                if (school == null || school.isBlank()) {
                    continue;
                }
                if (!submissionRepository.existsByEmailAndAuditTypeAndAcademicYearAndVersion(user.getEmail(), "academic", next, 1)) {
                    createBlankV1(user, "academic", school, null, next);
                    academicFormsCreated++;
                }
            } else if ("administrative".equals(role)) {
                String post = normalize(user.getPost());
                if (post == null || post.isBlank()) {
                    continue;
                }
                if (!submissionRepository.existsByEmailAndAuditTypeAndAcademicYearAndVersion(user.getEmail(), "administrative", next, 1)) {
                    createBlankV1(user, "administrative", "Administrative Office", post, next);
                    administrativeFormsCreated++;
                }
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("academicYear", next);
        response.put("auditCycle", toAuditCycle(next));
        response.put("previousAcademicYear", current);
        response.put("academicFormsCreated", academicFormsCreated);
        response.put("administrativeFormsCreated", administrativeFormsCreated);
        return response;
    }

    private void createBlankV1(User user, String auditType, String school, String post, String academicYear) {
        Submission submission = Submission.builder()
                .email(user.getEmail())
                .auditType(auditType)
                .school(school)
                .administrativePost(post)
                .submittedBy(user.getName())
                .status("DRAFT")
                .version(1)
                .academicYear(academicYear)
                .auditCycle(toAuditCycle(academicYear))
                .reportCategory("INTERNAL")
                .schoolGroup("academic".equals(auditType) ? SchoolUtils.schoolGroup(school) : null)
                .valuesData("{}")
                .tablesData("{}")
                .attachments("[]")
                .hasNextCycle(false)
                .build();
        Submission saved = submissionRepository.save(submission);
        saved.setRootSubmissionId(saved.getId());
        submissionRepository.save(saved);
    }

    private void validateReviewer(User caller) {
        String role = normalize(caller.getRole());
        if (!List.of("iqac", "vice-chancellor").contains(role)) {
            throw new SecurityException("Only IQAC or VC can perform this action");
        }
    }

    private void validateNextYear(String current, String next) {
        int[] currentParts = parseYear(current);
        int[] nextParts = parseYear(next);
        if (currentParts[1] != currentParts[0] + 1 || nextParts[1] != nextParts[0] + 1
                || nextParts[0] != currentParts[0] + 1 || nextParts[1] != currentParts[1] + 1) {
            throw new IllegalArgumentException("Academic year must increment exactly once");
        }
    }

    private int[] parseYear(String value) {
        String[] parts = value.split("-");
        if (parts.length != 2 || parts[0].length() != 4 || parts[1].length() != 4) {
            throw new IllegalArgumentException("Academic year must use format YYYY-YYYY");
        }
        try {
            return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Academic year must use numeric format YYYY-YYYY");
        }
    }

    private String normalizeYear(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Academic year is required");
        }
        return value.trim();
    }

    private String toAuditCycle(String academicYear) {
        int[] parts = parseYear(academicYear);
        return parts[0] + "-" + String.valueOf(parts[1]).substring(2);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }
}
