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
import java.util.Optional;
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
                    .accountType(validatedUser.accountType)
                    .category(validatedUser.category)
                    .auditorType(validatedUser.auditorType)
                    .auditorRole(validatedUser.auditorRole)
                    .post(validatedUser.post)
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

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(Authentication authentication, @PathVariable String id) {
        ResponseEntity<?> authorizationError = authorizeIqacForDelete(authentication);
        if (authorizationError != null) {
            return authorizationError;
        }

        Long userId;
        try {
            userId = Long.valueOf(id);
        } catch (NumberFormatException e) {
            return deleteError(HttpStatus.BAD_REQUEST, "Invalid user id");
        }

        return userService.findById(userId)
                .map(user -> {
                    if (!isManagedUser(user)) {
                        return deleteError(HttpStatus.FORBIDDEN, "You are not authorized to delete users");
                    }

                    userService.deleteUser(user);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "User deleted successfully"));
                })
                .orElseGet(() -> deleteError(HttpStatus.NOT_FOUND, "User not found"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            Authentication authentication,
            @PathVariable String id,
            @RequestBody(required = false) CreateUserRequest request) {
        ResponseEntity<?> authorizationError = authorizeIqacForUpdate(authentication);
        if (authorizationError != null) {
            return authorizationError;
        }

        Long userId;
        try {
            userId = Long.valueOf(id);
        } catch (NumberFormatException e) {
            return updateError(HttpStatus.BAD_REQUEST, "Invalid user id");
        }

        Optional<User> existingUser = userService.findById(userId);
        if (existingUser.isEmpty()) {
            return updateError(HttpStatus.NOT_FOUND, "User not found");
        }

        User user = existingUser.get();
        if (!isManagedUser(user)) {
            return updateError(HttpStatus.FORBIDDEN, "You are not authorized to update users");
        }

        try {
            ValidatedUser validatedUser = validateUpdateUserRequest(request);
            Optional<User> userWithEmail = userService.findByEmail(validatedUser.email);
            if (userWithEmail.isPresent() && !userWithEmail.get().getId().equals(user.getId())) {
                return updateError(HttpStatus.CONFLICT, "Email already exists.");
            }

            user.setName(validatedUser.name);
            user.setEmail(validatedUser.email);
            user.setRole(validatedUser.role);
            user.setSchool(validatedUser.school);
            user.setDesignation(validatedUser.designation);
            user.setAccountType(validatedUser.accountType);
            user.setCategory(validatedUser.category);
            user.setAuditorType(validatedUser.auditorType);
            user.setAuditorRole(validatedUser.auditorRole);
            user.setPost(validatedUser.post);

            User savedUser = userService.updateUser(user, validatedUser.password);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "User updated successfully",
                    "user", toUserResponse(savedUser)));
        } catch (IllegalArgumentException e) {
            return updateError(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return updateError(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error.");
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

    private ResponseEntity<?> authorizeIqacForDelete(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User user)) {
            return deleteError(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }

        if (!"iqac".equals(normalize(user.getRole()))) {
            return deleteError(HttpStatus.FORBIDDEN, "You are not authorized to delete users");
        }

        return null;
    }

    private ResponseEntity<?> authorizeIqacForUpdate(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User user)) {
            return updateError(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }

        if (!"iqac".equals(normalize(user.getRole()))) {
            return updateError(HttpStatus.FORBIDDEN, "You are not authorized to update users");
        }

        return null;
    }

    private ValidatedUser validateCreateUserRequest(CreateUserRequest request) {
        return validateUserRequest(request, true);
    }

    private ValidatedUser validateUpdateUserRequest(CreateUserRequest request) {
        return validateUserRequest(request, false);
    }

    private ValidatedUser validateUserRequest(CreateUserRequest request, boolean passwordRequired) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }

        String name = clean(request.getName());
        String email = normalize(request.getEmail());
        String password = request.getPassword();
        
        String reqAccountType = request.getAccountType() != null ? request.getAccountType() : request.getUserType();
        String accountType = normalize(reqAccountType);
        
        String reqCategory = request.getCategory() != null ? request.getCategory() : request.getAuditCategory();
        String category = normalize(reqCategory);
        
        String auditorType = normalize(request.getAuditorType());
        
        String reqAuditorRole = request.getAuditorRole() != null ? request.getAuditorRole() : request.getRole();
        String auditorRole = normalize(reqAuditorRole);
        
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
        if (passwordRequired && isBlank(password)) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (!isBlank(password) && password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }

        boolean isAuditor = "auditor".equals(accountType) || (auditorRole != null && auditorRole.contains("auditor")) || (role != null && role.contains("auditor"));

        if (isAuditor) {
            accountType = "auditor";
            
            if (isBlank(category)) {
                if (auditorRole != null && auditorRole.contains("academic")) {
                    category = "academic";
                } else if (auditorRole != null && auditorRole.contains("administrative")) {
                    category = "administrative";
                } else {
                    throw new IllegalArgumentException("Category (academic/administrative) is required for auditors.");
                }
            }
            
            if (isBlank(auditorType)) {
                if (auditorRole != null && auditorRole.contains("external")) {
                    auditorType = "external";
                } else {
                    auditorType = "internal";
                }
            }
            
            if (isBlank(auditorRole)) {
                auditorRole = category + "-" + auditorType + "-auditor";
            }
            role = auditorRole;
            
            if ("academic".equals(category)) {
                if (isBlank(school)) {
                    throw new IllegalArgumentException("School is required for academic auditors.");
                }
                if (!ACADEMIC_SCHOOLS.contains(school)) {
                    throw new IllegalArgumentException("Invalid academic school.");
                }
                post = null;
                if (isBlank(designation)) {
                    designation = (auditorType.substring(0, 1).toUpperCase() + auditorType.substring(1)) + " Academic Auditor";
                }
            } else if ("administrative".equals(category)) {
                school = ADMINISTRATIVE_OFFICE;
                if (isBlank(post)) {
                    throw new IllegalArgumentException("Post is required for administrative auditors.");
                }
                String mappedDesignation = ADMINISTRATIVE_POSTS.get(post);
                if (mappedDesignation == null) {
                    throw new IllegalArgumentException("Invalid administrative post.");
                }
                if (isBlank(designation)) {
                    designation = (auditorType.substring(0, 1).toUpperCase() + auditorType.substring(1)) + " " + mappedDesignation + " Auditor";
                }
            } else {
                throw new IllegalArgumentException("Invalid category for auditor.");
            }
            
            return new ValidatedUser(name, email, cleanPassword(password), role, school, designation, accountType, category, auditorType, auditorRole, post);
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
            return new ValidatedUser(name, email, cleanPassword(password), "director", school, isBlank(designation) ? "Director" : designation, "user", "academic", null, null, null);
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
            return new ValidatedUser(name, email, cleanPassword(password), "administrative", school, mappedDesignation, "user", "administrative", null, null, post);
        }

        throw new IllegalArgumentException("Invalid category.");
    }

    private boolean isManagedUser(User user) {
        String role = normalize(user.getRole());
        String accountType = normalize(user.getAccountType());
        return "director".equals(role) || "administrative".equals(role) || "auditor".equals(accountType) || (role != null && role.contains("auditor"));
    }

    private Map<String, Object> toUserResponse(User user) {
        String role = normalize(user.getRole());
        String accountType = normalize(user.getAccountType());
        if (isBlank(accountType)) {
            accountType = (role != null && role.contains("auditor")) ? "auditor" : "user";
        }
        
        String category = user.getCategory();
        if (isBlank(category)) {
            if ("director".equals(role)) {
                category = "academic";
            } else if ("administrative".equals(role)) {
                category = "administrative";
            } else if (role != null && role.contains("academic")) {
                category = "academic";
            } else if (role != null && role.contains("administrative")) {
                category = "administrative";
            } else {
                category = "";
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("category", category);
        response.put("role", role);
        response.put("school", user.getSchool());
        response.put("designation", user.getDesignation());
        response.put("post", user.getPost() != null ? user.getPost() : ("administrative".equals(role) ? getPostForDesignation(user.getDesignation()) : null));
        
        response.put("accountType", accountType);
        response.put("auditorType", user.getAuditorType());
        response.put("auditorRole", user.getAuditorRole());
        response.put("status", user.getStatus() != null ? user.getStatus() : "active");
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

    private ResponseEntity<Map<String, Object>> deleteError(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "success", false,
                "message", message));
    }

    private ResponseEntity<Map<String, Object>> updateError(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "success", false,
                "message", message));
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String cleanPassword(String value) {
        return isBlank(value) ? null : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ValidatedUser(
        String name, String email, String password, String role, String school, String designation,
        String accountType, String category, String auditorType, String auditorRole, String post
    ) {}

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
        private String accountType;
        private String userType;
        private String auditCategory;
        private String auditorType;
        private String auditorRole;
    }
}
