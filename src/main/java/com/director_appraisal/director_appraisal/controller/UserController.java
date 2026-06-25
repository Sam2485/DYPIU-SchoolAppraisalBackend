package com.director_appraisal.director_appraisal.controller;

import com.director_appraisal.director_appraisal.model.User;
import com.director_appraisal.director_appraisal.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin
public class UserController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE);

    private static final Set<String> ACADEMIC_SCHOOLS = Set.of(
            "School of Computer Science & Applications",
            "School of Bio-Engineering & Bio Science",
            "School of Continual Education",
            "School of Engineering, Management & Research",
            "School of Commerce & Management",
            "School of Media & Communication Studies",
            "School of Design",
            "School of Applied Arts");

    private static final String ADMINISTRATIVE_OFFICE = "Administrative Office";

    private static final Map<String, String> ADMINISTRATIVE_POSTS = Map.of(
            "registrar", "Registrar",
            "hr", "HR",
            "dean-student-welfare", "Dean Student Welfare",
            "dean-placement", "Dean Placement");

    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getUsers(Authentication authentication) {
        ResponseEntity<?> authorizationError = authorizeIqac(authentication);
        if (authorizationError != null) {
            return authorizationError;
        }

        List<Map<String, Object>> users = userService.findAllUsers().stream()
                .filter(this::isManagedUser)
                .map(this::toUserResponse)
                .toList();

        return ResponseEntity.ok(Map.of("users", users));
    }

    @PostMapping
    public ResponseEntity<?> createUser(Authentication authentication, @RequestBody(required = false) CreateUserRequest request) {
        ResponseEntity<?> authorizationError = authorizeIqac(authentication);
        if (authorizationError != null) {
            return authorizationError;
        }

        try {
            ValidatedUser validatedUser = validateCreateUserRequest(request);
            if (userService.findByEmail(validatedUser.email).isPresent()) {
                return error(HttpStatus.CONFLICT, "Email already exists.");
            }

            User savedUser = userService.createUser(User.builder()
                    .name(validatedUser.name)
                    .email(validatedUser.email)
                    .password(validatedUser.password)
                    .role(validatedUser.role)
                    .school(validatedUser.school)
                    .designation(validatedUser.designation)
                    .build());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "User created successfully",
                    "user", toUserResponse(savedUser)));
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase(Locale.ROOT).contains("already exists")) {
                return error(HttpStatus.CONFLICT, "Email already exists.");
            }
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error.");
        }
    }

    private ResponseEntity<?> authorizeIqac(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User user)) {
            return error(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }

        if (!"iqac".equals(normalize(user.getRole()))) {
            return error(HttpStatus.FORBIDDEN, "Only IQAC users can access this resource.");
        }

        return null;
    }

    private ValidatedUser validateCreateUserRequest(CreateUserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }

        String name = clean(request.getName());
        String email = normalize(request.getEmail());
        String password = request.getPassword();
        String category = normalize(request.getCategory());
        String role = normalize(request.getRole());
        String school = clean(request.getSchool());
        String designation = clean(request.getDesignation());
        String post = normalize(request.getPost());

        if (isBlank(name)) {
            throw new IllegalArgumentException("Name is required.");
        }
        if (isBlank(email)) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Email must be valid.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
        if (isBlank(category)) {
            throw new IllegalArgumentException("Category is required.");
        }

        if ("academic".equals(category)) {
            if (!"director".equals(role)) {
                throw new IllegalArgumentException("Academic category must use role director.");
            }
            if (isBlank(school)) {
                throw new IllegalArgumentException("School is required.");
            }
            if (!ACADEMIC_SCHOOLS.contains(school)) {
                throw new IllegalArgumentException("Invalid academic school.");
            }
            return new ValidatedUser(name, email, password, "director", school, isBlank(designation) ? "Director" : designation);
        }

        if ("administrative".equals(category)) {
            if (!"administrative".equals(role)) {
                throw new IllegalArgumentException("Administrative category must use role administrative.");
            }
            if (isBlank(school)) {
                throw new IllegalArgumentException("School is required.");
            }
            if (!ADMINISTRATIVE_OFFICE.equals(school)) {
                throw new IllegalArgumentException("Administrative category must use school Administrative Office.");
            }
            if (isBlank(post)) {
                throw new IllegalArgumentException("Post is required.");
            }
            String mappedDesignation = ADMINISTRATIVE_POSTS.get(post);
            if (mappedDesignation == null) {
                throw new IllegalArgumentException("Invalid administrative post.");
            }
            if (!isBlank(designation) && !mappedDesignation.equals(designation)) {
                throw new IllegalArgumentException("Designation must match selected administrative post.");
            }
            return new ValidatedUser(name, email, password, "administrative", school, mappedDesignation);
        }

        throw new IllegalArgumentException("Invalid category.");
    }

    private boolean isManagedUser(User user) {
        String role = normalize(user.getRole());
        return "director".equals(role) || "administrative".equals(role);
    }

    private Map<String, Object> toUserResponse(User user) {
        String role = normalize(user.getRole());
        String category = "director".equals(role) ? "academic" : "administrative";

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("category", category);
        response.put("role", role);
        response.put("school", user.getSchool());
        response.put("designation", user.getDesignation());
        response.put("post", "administrative".equals(role) ? getPostForDesignation(user.getDesignation()) : null);
        response.put("status", "active");
        return response;
    }

    private String getPostForDesignation(String designation) {
        return ADMINISTRATIVE_POSTS.entrySet().stream()
                .filter(entry -> entry.getValue().equals(designation))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ValidatedUser(String name, String email, String password, String role, String school, String designation) {
    }

    @Data
    public static class CreateUserRequest {
        private String category;
        private String role;
        private String school;
        private String designation;
        private String post;
        private String name;
        private String email;
        private String password;
    }
}
