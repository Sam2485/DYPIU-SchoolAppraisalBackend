# Security & Authorization Documentation

This document describes the authentication and security implementation of the School Appraisal Backend API.

## Core Security Technologies
- **Authentication Engine**: Spring Security 6.x.
- **Token Mechanism**: JSON Web Token (JWT) using `io.jsonwebtoken` (jjwt) version 0.12.x.
- **Hashing Algorithm**: BCrypt (strength 10) for password storage.
- **Session Policy**: Stateless (`SessionCreationPolicy.STATELESS`).

---

## 1. Role-Based Access Control (RBAC)

The system maps authorization checks dynamically using standard user roles. 

### User Role Storage vs. Authority Mapping
- **Database Representation**: Roles are stored in the database as lowercase strings in the `role` column of the `users` table (e.g. `"director"`, `"administrative"`, `"vice-chancellor"`, `"iqac"`, or specific auditor roles like `"academic-internal-auditor"`).
- **Granted Authorities Mapping**: In `User.java` (implementing `UserDetails`), roles are dynamically converted to Spring Security granted authorities by converting to uppercase and prefixing with `"ROLE_"`:
  ```java
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
      return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
  }
  ```
  This maps database roles to:
  - `"director"` ➔ `ROLE_DIRECTOR`
  - `"administrative"` ➔ `ROLE_ADMINISTRATIVE`
  - `"vice-chancellor"` ➔ `ROLE_VICE-CHANCELLOR`
  - `"iqac"` ➔ `ROLE_IQAC`
  - `"academic-internal-auditor"` ➔ `ROLE_ACADEMIC-INTERNAL-AUDITOR`
  - `"academic-external-auditor"` ➔ `ROLE_ACADEMIC-EXTERNAL-AUDITOR`
  - `"administrative-internal-auditor"` ➔ `ROLE_ADMINISTRATIVE-INTERNAL-AUDITOR`
  - `"administrative-external-auditor"` ➔ `ROLE_ADMINISTRATIVE-EXTERNAL-AUDITOR`

### Endpoint Rules & Authorization Matrix:
- `/api/auth/**`: Allowed publicly without token.
- `/uploads/**`: Allowed publicly (serves local static media assets).
- `/api/submissions/all`: Secured via **Method-Level Security** (`@PreAuthorize("hasAnyRole('ROLE_VICE-CHANCELLOR', 'ROLE_IQAC', 'ROLE_ACADEMIC-INTERNAL-AUDITOR', 'ROLE_ACADEMIC-EXTERNAL-AUDITOR', 'ROLE_ADMINISTRATIVE-INTERNAL-AUDITOR', 'ROLE_ADMINISTRATIVE-EXTERNAL-AUDITOR')")`). Only VC, IQAC, or assigned/matched auditors can query all submissions. Submissions lists are filtered based on the calling user's role:
  - **IQAC**: returns forms with status in `SUBMITTED`, `UNDER_REVIEW`, `AUDITOR_COMPLETED`, `APPROVED`, `SENT_BACK`.
  - **VC**: returns forms only if status is `AUDITOR_COMPLETED` or `APPROVED` (hidden before audit completion).
  - **Auditors**: returns only submissions where the auditor is explicitly assigned (listed in `forwardedToAuditorIds`/`forwardedToAuditorEmails`) or fallback matched (category, auditorType, and school/post match) with status `UNDER_REVIEW` or `AUDITOR_COMPLETED`.
- `/api/submissions/{id}` and `/api/submissions/{id}/snapshots`: Access is authorized based on context checking:
  - Owners can view their own forms.
  - IQAC can view all forms.
  - VC can view forms ONLY if status is `AUDITOR_COMPLETED` or `APPROVED` or `SENT_BACK` (access blocked otherwise).
  - Auditors can view forms ONLY if they are explicitly assigned or fallback matched.
- `/api/submissions/{id}/review`: Secured via **Method-Level Security** (`@PreAuthorize("hasAnyRole('ROLE_VICE-CHANCELLOR', 'ROLE_IQAC')")`). Only VC or IQAC can review. Review is blocked unless the submission status is `AUDITOR_COMPLETED`.
- `/api/users/**`: Secured via **Manual Controller-Level Authorization Checks**. Every operation (GET, POST, PUT, DELETE) inside `UserController.java` manually checks that the authenticated user has the `"iqac"` role. If the check fails, the API responds with `403 Forbidden` (`You are not authorized to update/delete users` or `Only IQAC users can access this resource.`).
- All other endpoints (including submission drafts/saving): Requires a valid authenticated user session (verified by the JWT authentication filter).
- `/api/submissions/{id}` updates: Owners can update drafts. Auditors can edit only Part E/F auditor fields and set status to `AUDITOR_COMPLETED`. IQAC can edit forwarding fields. All edits blocked if form is `APPROVED`.

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
To protect user accounts from token harvesting, the system secures reset tokens using a one-way hashing design and conditional response protection:
1. **Token Generation:** The system generates a cryptographically secure random UUID token (`rawToken`) when a user requests a password reset.
2. **One-Way Hashing:** The system hashes the `rawToken` using `SHA-256` before writing the hash value to the `password_reset_tokens` table.
3. **Verification:** When the user clicks the reset link in their email and submits a new password, the system hashes the token parameter received from the client and queries the database using the hash. This prevents database-read access from compromising active tokens since the raw tokens are never written to disk.
4. **Token Clean-up:** The system automatically purges expired or old reset tokens associated with the target email whenever a new reset request is made.
5. **Conditional Response Hardening (Production vs Development):**
   - **GCP Production Mode (`app.gcp.enabled=true`):** The raw token is strictly omitted from the API response payload in `/forgot-password`. Users must rely solely on the reset link received in their email.
   - **Local Development Mode (`app.gcp.enabled=false`):** The raw token is returned inside the HTTP response under the `"token"` key to facilitate testing and mock mailer integration without requiring active mailbox configuration.


