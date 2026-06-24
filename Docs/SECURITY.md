# Security & Authorization Documentation

This document describes the authentication and security implementation of the School Appraisal Backend API.

## Core Security Technologies
- **Authentication Engine**: Spring Security 6.x.
- **Token Mechanism**: JSON Web Token (JWT) using `io.jsonwebtoken` (jjwt) version 0.12.x.
- **Hashing Algorithm**: BCrypt (strength 10) for password storage.
- **Session Policy**: Stateless (`SessionCreationPolicy.STATELESS`).

---

## 1. Role-Based Access Control (RBAC)

The system maps authorization checks dynamically using standard user roles:
- **`ROLE_DIRECTOR`**: Academic school directors (submits Academic Audit forms).
- **`ROLE_ADMINISTRATIVE`**: Administrative staff members (submits Administrative Audit forms).
- **`ROLE_VICE-CHANCELLOR`**: Vice Chancellor (reviewer/viewer role).
- **`ROLE_IQAC`**: Internal Quality Assurance Cell Coordinator (reviewer/viewer role).

### Endpoint Rules Matrix:
- `/api/auth/**`: Allowed publicly without token.
- `/h2-console/**`: Allowed publicly (H2 console bypass).
- `/uploads/**`: Allowed publicly (serves local static media assets).
- `/api/submissions/all`: Authorized for **`vice-chancellor`** and **`iqac`** only.
- `/api/submissions/{id}/review`: Authorized for **`vice-chancellor`** and **`iqac`** only.
- All other endpoints: Requires valid authenticated user session (directors, administrative users, or reviewers).

---

## 2. JWT Configuration
Tokens are stateless and hold user profile details inside private claims. 

### JWT Token Claims Schema:
- Subject (`sub`): Email address of the user.
- Issued At (`iat`): Date-time of creation.
- Expiration (`exp`): 24 hours after creation.
- Custom Profile Parameters:
  - `name`: User's display name.
  - `designation`: Submitter's title (e.g. Registrar).
  - `school`: Submitter's school/office.
  - `role`: Auth role of the profile.

---

## 3. JWT Request Processing Flow

```
HTTP Request
     │
     ▼
┌────────────────────────┐
│   JwtRequestFilter     │ <── Checks for 'Authorization: Bearer <token>' header
└──────────┬─────────────┘
           │ (If token valid)
           ▼
┌────────────────────────┐
│ SecurityContextHolder  │ <── Mapped inside Security context of thread
└──────────┬─────────────┘
           │
           ▼
┌────────────────────────┐
│   SecurityFilterChain  │ <── Verifies HTTP endpoints rules and roles
└────────────────────────┘
```

---

## 4. CORS Policy
To allow seamless connection with frontends (e.g., Vite/React developer servers running on localhost:5173), CORS is configured on the security level to:
- Allowed Origins: Matches wildcard expressions or patterns (`*`).
- Allowed HTTP Methods: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`, `PATCH`.
- Allowed Headers: `Authorization`, `Content-Type`, `Cache-Control`.
- Exposed Headers: `Authorization` (allowing the client to read token headers).
- Allow Credentials: `true`.

---

## 5. Password Reset Security
To protect user accounts from token harvesting (if the database is compromised), the system secures reset tokens using a one-way hashing design:
1. **Token Generation:** The system generates a cryptographically secure random UUID token (`rawToken`) when a user requests a password reset.
2. **One-Way Hashing:** The system hashes the `rawToken` using `SHA-256` before writing the hash value to the `password_reset_tokens` table.
3. **Verification:** When the user clicks the reset link in their email and submits a new password, the system hashes the token parameter received from the client and queries the database using the hash. 
This prevents database-read access from compromising active tokens since the raw tokens are never written to disk.
4. **Token Clean-up:** The system automatically purges expired or old reset tokens associated with the target email whenever a new reset request is made.

