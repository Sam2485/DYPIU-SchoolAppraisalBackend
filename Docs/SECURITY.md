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

---

## 6. Administrative Auditor Post-Level Access Control

To restrict auditor privileges within the shared administrative audit form, the backend enforces fine-grained section-level authorization:

### 1. Token Claims & Profile Payload
- During login and profile requests, the server resolves all administrative posts assigned to the auditor from the `user_administrative_posts` table (and falls back to their single `post` field).
- This is returned as the `administrativePosts` array in login claims and user response payloads, which the frontend uses to render fields as editable or read-only.

### 2. Dynamically Injected Permissions Map
When fetching a submission (`GET /api/submissions/{id}` and `GET /api/submissions/all`), the backend calculates and embeds a transient `permissions` object:
```json
{
  "canView": true,
  "editablePosts": ["library", "examination"],
  "readOnlyPosts": ["registrar", "hr", "dean-student-welfare", "dean-placement", "accounts"],
  "permissions": {
    "library": { "canEdit": true },
    "examination": { "canEdit": true },
    "registrar": { "canEdit": false },
    "hr": { "canEdit": false },
    "dean-student-welfare": { "canEdit": false },
    "dean-placement": { "canEdit": false },
    "accounts": { "canEdit": false }
  }
}
```

### 3. Backend Payload Validation & Protection
To prevent unauthorized field updates (e.g. through API payload tampering), the backend inspects any incoming update request (`PUT /api/submissions/{id}`):
- It parses incoming `valuesData` and `tablesData` fields.
- For every updated key, it classifies which administrative post/role owns that field or section (using `resolvePostForKey`).
- If an auditor attempts to save a change to a key belonging to a post they are **not** explicitly assigned to, the request is instantly rejected by throwing a `SecurityException`, which the global exception handler returns as an **HTTP 403 Forbidden** error.
- All VC, IQAC, and Director flows are bypassed, preserving their standard workspace permissions.

### 4. Administrative Auditor Forwarding & Visibility
To correctly route shared administrative audits to specialized auditors:
- When IQAC forwards a submission (`PUT /api/submissions/{id}` with status `UNDER_REVIEW`), the backend validates the assignment using the auditor's administrative posts instead of generic display labels (e.g., "Administrative Office") or email/name fields.
- The system resolves the submission's active posts from:
  - `request.forwardedAdministrativePosts`
  - `submission.administrativeProgress` keys (with status submitted/approved/under-review/auditor-completed)
  - `valuesData.__administrativeSubmissionStatus` keys (where submitted=true)
- It verifies that the selected auditor has at least one post overlapping with the submission's active posts.
- For visibility (`GET /api/submissions/all`), an administrative auditor can see submissions in the dashboard if there is a post overlap OR if they are directly assigned by ID/Email.

### 5. Administrative Audit External-Cycle Workflow
When transitioning the administrative audit to the External cycle:
- Creating the next cycle (`version: 2`, `reportCategory: "EXTERNAL"`) resets the `administrativeProgress` of all posts (`registrar`, `hr`, `dean-student-welfare`, `dean-placement`) to `DRAFT` and removes the `__administrativeSubmissionStatus` tracking object.
- This unlocks the respective sections, allowing administrative contributors to edit and resubmit their assigned parts for the external cycle without affecting the archived Internal version.
- To prevent early forwarding, the backend blocks forwarding to external auditors until all four required administrative contributors have submitted their sections.



