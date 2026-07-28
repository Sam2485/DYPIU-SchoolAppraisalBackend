package com.director_appraisal.director_appraisal.controller;

import com.director_appraisal.director_appraisal.model.User;
import com.director_appraisal.director_appraisal.model.UserAdministrativePost;
import com.director_appraisal.director_appraisal.repository.UserAdministrativePostRepository;
import com.director_appraisal.director_appraisal.service.AcademicYearService;
import com.director_appraisal.director_appraisal.service.JwtService;
import com.director_appraisal.director_appraisal.service.UserService;
import com.director_appraisal.director_appraisal.service.RateLimiterService;
import com.director_appraisal.director_appraisal.service.MfaService;
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
    private final RateLimiterService rateLimiterService;
    private final MfaService mfaService;
    private final com.director_appraisal.director_appraisal.repository.MfaLoginSessionRepository mfaLoginSessionRepository;
    private final jakarta.servlet.http.HttpServletRequest httpServletRequest;

    @org.springframework.beans.factory.annotation.Value("${app.gcp.enabled:false}")
    private boolean gcpEnabled;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        if (loginRequest == null || loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username and password are required."));
        }
        String email = loginRequest.getUsername().trim().toLowerCase();
        String password = loginRequest.getPassword();

        String clientIp = rateLimiterService.getClientIp(httpServletRequest);
        String ipKey = "login:ip:" + clientIp;
        String userKey = "login:user:" + email;

        RateLimiterService.RateLimitResult rateLimitResult = rateLimiterService.checkLimit(ipKey, userKey, "login");
        if (!rateLimitResult.allowed) {
            org.slf4j.LoggerFactory.getLogger(AuthController.class).warn(
                "Rate limit exceeded for endpoint: login, client IP: {}, user: {}, timestamp: {}",
                clientIp, email, java.time.Instant.now()
            );
            return ResponseEntity.status(429)
                    .header("X-RateLimit-Limit", String.valueOf(rateLimitResult.limit))
                    .header("X-RateLimit-Remaining", String.valueOf(rateLimitResult.remaining))
                    .header("Retry-After", String.valueOf(rateLimitResult.retryAfter))
                    .body(Map.of(
                            "success", false,
                            "message", "Too many requests. Please try again after one minute."
                    ));
        }

        User user = userService.findByEmail(email)
                .orElse(null);

        if (user == null || !userService.checkPassword(password, user.getPassword())) {
            return ResponseEntity.badRequest()
                    .header("X-RateLimit-Limit", String.valueOf(rateLimitResult.limit))
                    .header("X-RateLimit-Remaining", String.valueOf(rateLimitResult.remaining))
                    .body(Map.of("message", "Invalid email address or password."));
        }

        String loginSessionId = mfaService.createMfaSession(user);

        return ResponseEntity.ok()
                .header("X-RateLimit-Limit", String.valueOf(rateLimitResult.limit))
                .header("X-RateLimit-Remaining", String.valueOf(rateLimitResult.remaining))
                .body(Map.of(
                        "mfaRequired", true,
                        "loginSessionId", loginSessionId,
                        "expiresIn", 300
                ));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest verifyRequest) {
        if (verifyRequest == null || verifyRequest.getLoginSessionId() == null || verifyRequest.getOtp() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "loginSessionId and otp are required."));
        }

        try {
            Long userId = mfaService.verifyOtp(verifyRequest.getLoginSessionId().trim(), verifyRequest.getOtp().trim());
            User user = userService.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found."));

            return buildLoginResponse(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(429).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", safeMessage(e, "Verification failed.")));
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody ResendOtpRequest resendRequest) {
        if (resendRequest == null || resendRequest.getLoginSessionId() == null || resendRequest.getLoginSessionId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "loginSessionId is required."));
        }

        String clientIp = rateLimiterService.getClientIp(httpServletRequest);
        String ipKey = "resend:ip:" + clientIp;
        String sessionKey = "resend:session:" + resendRequest.getLoginSessionId();

        RateLimiterService.RateLimitResult rateLimitResult = rateLimiterService.checkLimit(ipKey, sessionKey, "resend");
        if (!rateLimitResult.allowed) {
            return ResponseEntity.status(429)
                    .header("Retry-After", String.valueOf(rateLimitResult.retryAfter))
                    .body(Map.of("message", "Too many resend requests. Please try again after one minute."));
        }

        try {
            com.director_appraisal.director_appraisal.model.MfaLoginSession session = mfaLoginSessionRepository
                    .findById(resendRequest.getLoginSessionId().trim())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid login session."));
            User user = userService.findById(session.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found."));

            mfaService.resendOtp(resendRequest.getLoginSessionId().trim(), user.getEmail());
            return ResponseEntity.ok(Map.of("message", "Verification code resent successfully.", "expiresIn", 300));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", safeMessage(e, "Could not resend verification code.")));
        }
    }

    private ResponseEntity<LoginResponse> buildLoginResponse(User user) {
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

        String trimmedEmail = email.trim().toLowerCase();
        String clientIp = rateLimiterService.getClientIp(httpServletRequest);
        String ipKey = "forgot:ip:" + clientIp;
        String userKey = "forgot:user:" + trimmedEmail;

        RateLimiterService.RateLimitResult rateLimitResult = rateLimiterService.checkLimit(ipKey, userKey, "forgot");
        if (!rateLimitResult.allowed) {
            org.slf4j.LoggerFactory.getLogger(AuthController.class).warn(
                "Rate limit exceeded for endpoint: forgot-password, client IP: {}, user: {}, timestamp: {}",
                clientIp, trimmedEmail, java.time.Instant.now()
            );
            return ResponseEntity.status(429)
                    .header("X-RateLimit-Limit", String.valueOf(rateLimitResult.limit))
                    .header("X-RateLimit-Remaining", String.valueOf(rateLimitResult.remaining))
                    .header("Retry-After", String.valueOf(rateLimitResult.retryAfter))
                    .body(Map.of(
                            "success", false,
                            "message", "Too many requests. Please try again after one minute."
                    ));
        }

        try {
            String token = userService.createPasswordResetToken(trimmedEmail);
            if (gcpEnabled) {
                return ResponseEntity.ok()
                        .header("X-RateLimit-Limit", String.valueOf(rateLimitResult.limit))
                        .header("X-RateLimit-Remaining", String.valueOf(rateLimitResult.remaining))
                        .body(Map.of(
                                "message", "If that email is registered, a reset link has been generated."
                        ));
            } else {
                return ResponseEntity.ok()
                        .header("X-RateLimit-Limit", String.valueOf(rateLimitResult.limit))
                        .header("X-RateLimit-Remaining", String.valueOf(rateLimitResult.remaining))
                        .body(Map.of(
                                "message", "If that email is registered, a reset link has been generated.",
                                "token", token
                        ));
            }
        } catch (Exception e) {
            // Standard safety practice: return identical response even if user email doesn't exist
            return ResponseEntity.ok()
                    .header("X-RateLimit-Limit", String.valueOf(rateLimitResult.limit))
                    .header("X-RateLimit-Remaining", String.valueOf(rateLimitResult.remaining))
                    .body(Map.of(
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
    public static class VerifyOtpRequest {
        private String loginSessionId;
        private String otp;
    }

    @Data
    public static class ResendOtpRequest {
        private String loginSessionId;
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
