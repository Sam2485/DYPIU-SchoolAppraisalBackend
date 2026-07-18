package com.director_appraisal.director_appraisal.controller;

import com.director_appraisal.director_appraisal.model.User;
import com.director_appraisal.director_appraisal.model.UserAdministrativePost;
import com.director_appraisal.director_appraisal.repository.UserAdministrativePostRepository;
import com.director_appraisal.director_appraisal.service.AcademicYearService;
import com.director_appraisal.director_appraisal.service.JwtService;
import com.director_appraisal.director_appraisal.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AcademicYearService academicYearService;
    private final UserAdministrativePostRepository userAdministrativePostRepository;

    @org.springframework.beans.factory.annotation.Value("${app.gcp.enabled:false}")
    private boolean gcpEnabled;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        if (loginRequest == null || loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username and password are required."));
        }
        String email = loginRequest.getUsername().trim().toLowerCase();
        String password = loginRequest.getPassword();

        User user = userService.findByEmail(email)
                .orElse(null);

        if (user == null || !userService.checkPassword(password, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid email address or password."));
        }

        String currentAcademicYear = academicYearService.getCurrentAcademicYearLabel();

        java.util.List<String> administrativePosts = getAdministrativePosts(user);
        String canonicalPost = canonicalAdministrativePost(user.getPost());
        String role = user.getRole();
        String school = isReviewerRole(role) ? null : user.getSchool();

        Map<String, Object> claims = new java.util.LinkedHashMap<>();
        putClaim(claims, "name", user.getName());
        putClaim(claims, "designation", user.getDesignation());
        putClaim(claims, "school", school);
        putClaim(claims, "role", role);
        putClaim(claims, "post", canonicalPost);
        putClaim(claims, "currentAcademicYear", currentAcademicYear);
        claims.put("administrativePosts", administrativePosts);

        // Generate JWT Token
        String token = jwtService.generateToken(user, claims);

        return ResponseEntity.ok(new LoginResponse(
                token,
                user.getEmail(),
                user.getName(),
                user.getDesignation(),
                school,
                role,
                user.getId(),
                user.getId(),
                user.getAccountType(),
                user.getCategory(),
                user.getAuditorType(),
                user.getAuditorRole(),
                canonicalPost,
                currentAcademicYear,
                administrativePosts
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required."));
        }

        try {
            String token = userService.createPasswordResetToken(email);
            if (gcpEnabled) {
                return ResponseEntity.ok(Map.of(
                        "message", "If that email is registered, a reset link has been generated."
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "message", "If that email is registered, a reset link has been generated.",
                        "token", token
                ));
            }
        } catch (Exception e) {
            // Standard safety practice: return identical response even if user email doesn't exist
            return ResponseEntity.ok(Map.of(
                    "message", "If that email is registered, a reset link has been generated."
            ));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");

        if (token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Token and newPassword are required."));
        }

        try {
            userService.resetPassword(token, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password has been reset successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", safeMessage(e, "Unable to reset password.")));
        }
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class LoginResponse {
        private final String token;
        private final String email;
        private final String name;
        private final String designation;
        private final String school;
        private final String role;
        private final Long id;
        private final Long userId;
        private final String accountType;
        private final String category;
        private final String auditorType;
        private final String auditorRole;
        private final String post;
        private final String currentAcademicYear;
        private final java.util.List<String> administrativePosts;
    }

    private java.util.List<String> getAdministrativePosts(User user) {
        if (user.getId() == null) {
            return java.util.List.of();
        }
        java.util.List<String> posts = userAdministrativePostRepository.findByUserId(user.getId()).stream()
                .map(UserAdministrativePost::getPost)
                .map(this::canonicalAdministrativePost)
                .filter(post -> post != null && !post.isBlank())
                .toList();
        if (!posts.isEmpty()) {
            return posts;
        }
        String role = user.getRole() != null ? user.getRole().toLowerCase() : "";
        String accountType = user.getAccountType() != null ? user.getAccountType().toLowerCase() : "";
        String category = user.getCategory() != null ? user.getCategory().toLowerCase() : "";
        if (("auditor".equals(accountType) || role.contains("auditor")) && "administrative".equals(category) && user.getPost() != null) {
            String canonicalPost = canonicalAdministrativePost(user.getPost());
            return canonicalPost != null ? java.util.List.of(canonicalPost) : java.util.List.of();
        }
        if ("administrative".equalsIgnoreCase(user.getCategory()) && user.getPost() != null) {
            String canonicalPost = canonicalAdministrativePost(user.getPost());
            return canonicalPost != null ? java.util.List.of(canonicalPost) : java.util.List.of();
        }
        if ("administrative".equals(role) && user.getPost() != null) {
            String canonicalPost = canonicalAdministrativePost(user.getPost());
            return canonicalPost != null ? java.util.List.of(canonicalPost) : java.util.List.of();
        }
        return java.util.List.of();
    }

    private String canonicalAdministrativePost(String post) {
        if (post == null || post.isBlank()) {
            return null;
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

    private void putClaim(Map<String, Object> claims, String key, Object value) {
        if (value != null) {
            claims.put(key, value);
        }
    }

    private boolean isReviewerRole(String role) {
        return "iqac".equalsIgnoreCase(role) || "vice-chancellor".equalsIgnoreCase(role);
    }

    private String safeMessage(Exception e, String fallback) {
        return e.getMessage() != null ? e.getMessage() : fallback;
    }
}
