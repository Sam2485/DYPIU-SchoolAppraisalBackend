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

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        String email = loginRequest.getUsername().trim().toLowerCase();
        String password = loginRequest.getPassword();

        User user = userService.findByEmail(email)
                .orElse(null);

        if (user == null || !userService.checkPassword(password, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid email address or password."));
        }

        String currentAcademicYear = academicYearService.getCurrentAcademicYearLabel();

        java.util.List<String> administrativePosts = getAdministrativePosts(user);

        // Generate JWT Token
        String token = jwtService.generateToken(user, Map.of(
                "name", user.getName(),
                "designation", user.getDesignation(),
                "school", user.getSchool(),
                "role", user.getRole(),
                "currentAcademicYear", currentAcademicYear,
                "administrativePosts", administrativePosts
        ));

        return ResponseEntity.ok(new LoginResponse(
                token,
                user.getEmail(),
                user.getName(),
                user.getDesignation(),
                user.getSchool(),
                user.getRole(),
                user.getId(),
                user.getId(),
                user.getAccountType(),
                user.getCategory(),
                user.getAuditorType(),
                user.getAuditorRole(),
                user.getPost(),
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
            // Returns the token directly in the response for development convenience
            return ResponseEntity.ok(Map.of(
                    "message", "If that email is registered, a reset link has been generated.",
                    "token", token
            ));
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
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
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
                .toList();
        if (!posts.isEmpty()) {
            return posts;
        }
        if ("administrative".equalsIgnoreCase(user.getCategory()) && user.getPost() != null) {
            return java.util.List.of(user.getPost());
        }
        return java.util.List.of();
    }
}
