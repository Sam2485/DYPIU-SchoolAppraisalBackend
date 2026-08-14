# Service Layer & Storage Workflows

This document describes the business services, transactional processes, and file storage workflows implemented in the School Appraisal Backend.

---

## 1. Core Services

#### UserService
Integrates with Spring Security's `UserDetailsService` and handles user authentication, profile CRUD operations, and password reset flows:
- `loadUserByUsername(String email)`: Dynamically retrieves a user from the database by email address.
- `findByEmail(String email)`: Checks for user existence by email.
- `findById(Long id)`: Fetches a user profile by database primary key.
- `findAllUsers()`: Retrieves a list of all user profiles registered in the system.
- `createUser(User user)`: Checks for email conflicts, encodes the raw password using BCrypt hashing, and persists the new user.
- `updateUser(User user, String rawPassword)`: Saves changes to name, email, role, school, and designation. If a non-blank `rawPassword` is supplied, it re-hashes and updates the user's password.
- `deleteUser(User user)`: Performs soft-deletion and year-scoped data cleanup to enforce historical appraisal data immutability:
  1. Applies soft-deletion (`setDeleted(true)`, `setDeletedAt(now)`), preserving user identity for historical reporting.
  2. Calls `SubmissionService.removeAdministrativeUserContribution` and `SubmissionService.deleteUserSubmissionsAndAttachments` strictly for records belonging to the CURRENT active working academic year (e.g. `2025-26`).
  3. Historical appraisal records from previous academic years are kept permanently IMMUTABLE and remain viewable via the year-selection dropdown.
  4. Clears reset tokens, auditor assignments, and administrative posts associated with the active academic year.
- `checkPassword(String rawPassword, String encodedPassword)`: Compares raw login inputs against BCrypt-encrypted database passwords.
- `createPasswordResetToken(String email)`: Performs password reset token generation:
  1. Locates the user or throws an exception.
  2. Wipes any stale reset tokens for the user's email.
  3. Generates a secure raw token via UUID.
  4. Hashes the raw token with SHA-256 to save in the database (protects token database leakage).
  5. Formulates a reset URL using the configured `FRONTEND_URL` and sends it to the user's email via `EmailService`.
- `resetPassword(String rawToken, String newPassword)`: Hashes the raw token from the link, queries the database, verifies that the token is valid, active, and unused, updates the user's password with BCrypt, and flags the reset token as used.

### RefreshTokenService
Manages the lifecycle of long-lived Refresh Tokens (7 days expiration) stored in PostgreSQL:
- `createRefreshToken(User user)`: Deletes any existing active refresh token for the user (enforces single active session per user), generates a secure UUID token string, sets expiry to 7 days (`Instant.now().plusMillis(604800000)`), and saves the entity to `refresh_tokens`.
- `findByToken(String token)`: Searches the repository for a matching refresh token record.
- `verifyExpiration(RefreshToken token)`: Validates that the refresh token is not revoked and its expiry date has not passed; automatically purges expired tokens from DB and throws an error if expired.
- `deleteByToken(String token)`: Deletes a refresh token upon explicit user logout.
- `deleteByUser(User user)`: Revokes all tokens associated with a user profile.
- `purgeExpiredTokens()`: Periodic cleanup function to purge all expired refresh tokens from the database.

### EmailService
Wraps Spring Boot's `JavaMailSender` to send simple transactional text emails.

### SubmissionService
Implements the core appraisal form lifecycle, draft management, and version/history snapshot capturing:
- `getOrCreateDraft(String email, String auditType)`: Retrieves the currently active draft for the submitter. If none exists, builds and saves a new blank submission record with status `DRAFT`.
- `saveDraft(String email, ...)`: Updates draft values, increments the document version counter, and triggers `createSnapshot`. Throws an error if the form has already been `APPROVED`.
- `submitForm(String email, ...)`: Updates values, overrides status to `SUBMITTED`, logs the `submittedAt` timestamp, increments the version counter, and triggers `createSnapshot`.
- `updateSubmission(Long id, User caller, String status, String forwardedAuditorType, ...)`: Performs updates on submissions. Validates caller authorizations (supports Owner, IQAC, and Assigned/Matched Auditors). Handles forwarding assignments, stamps auditor review details upon status changing to `AUDITOR_COMPLETED`, and triggers `createSnapshot`.
- `populateForwardingAuditors(Submission submission, String forwardedAuditorType)`: Automatically queries the database to find all matching auditor users based on `forwardedAuditorType` and target categories (school for academic audits, submitter's post for administrative audits), then serializes their IDs, names, and emails as JSON arrays into the plural fields (and defaults the legacy singular fields).
- `injectAuditorSignOff(String valuesData, User auditor)`: Parses the custom form data, injects the auditor's name, designation, role, and current timestamp into `__auditSignOff.auditedBy`, and re-serializes the values JSON payload.
- `getAllSubmissionsForUser(User user)`: Filters and returns submissions list depending on the user's role:
  - **IQAC**: returns submitted, under review, auditor completed, approved/sent-back forms.
  - **VC**: returns only auditor completed and approved forms.
  - **Auditors**: returns forms where the auditor is directly assigned or fallback matched (status must be `UNDER_REVIEW` or `AUDITOR_COMPLETED`).
- `reviewSubmission(Long id, String status, String remarks, String reviewerName)`: Invoked by VC/IQAC reviewers to set the final status (`APPROVED`, `SENT_BACK`, `UNDER_REVIEW`), logs reviewer remarks, and triggers `createSnapshot`. Blocks approvals or sending back unless status is already `AUDITOR_COMPLETED`.
- `getSnapshotsForSubmission(Long submissionId)`: Retrieves historical snapshots, ordered from newest to oldest.
- `createSnapshot(Submission submission)`: Prepares and writes a historical record to the `snapshots` table every time the submission state is saved, submitted, or reviewed.

---

## 2. File Upload & Attachment Download Engine

The **AttachmentService** & **SubmissionController** implement dual-storage file management and robust attachment ZIP package generation:

```
                   Attachment Package Generation Workflow
                                     │
                                     ▼
                ┌──────────────────────────────────────────┐
                │ Scan Submission Payloads for Attachments │
                │ (attachments, tablesData, & valuesData) │
                └────────────────────┬─────────────────────┘
                                     │
                                     ▼
                ┌──────────────────────────────────────────┐
                │    Deduplicate Unique Attachment List    │
                └────────────────────┬─────────────────────┘
                                     │
                                     ▼
                ┌──────────────────────────────────────────┐
                │ Parse Storage URL / Object Key Path      │
                │ (Strips GCP bucket prefixes, cleans path)│
                └────────────────────┬─────────────────────┘
                                     │
                                     ▼
                ┌──────────────────────────────────────────┐
                │ Direct / Relative File Resolution Check  │
                └──────────┬────────────────────┬──────────┘
                           │                    │
                    (File Found)         (File Not Found)
                           │                    │
                           ▼                    ▼
                ┌────────────────────┐ ┌───────────────────────────────────┐
                │ Stream File to ZIP │ │ Fuzzy Normalized Filename Search  │
                └────────────────────┘ │ (Strips UUIDs, spaces/punctuation)│
                                       └─────────────────┬─────────────────┘
                                                         │
                                                  (Match Found)
                                                         │
                                                         ▼
                                               ┌────────────────────┐
                                               │ Stream File to ZIP │
                                               └────────────────────┘
```

### Key Upload & Attachment Package Mechanics:
1. **Deduplication**: On upload, the service calculates the SHA-256 checksum of the file bytes. It builds the upload path as:
   `users/<userKey_hash>/attachments/<content_sha256>.pdf`
   If the file already exists at this path, the service rejects the request with an error to prevent duplicate storage.
2. **Multi-Source Payload Scanning**: When generating bulk ZIP downloads (`SubmissionController.downloadAttachments`), the backend recursively scans `attachments` (top-level array), `tablesData` (embedded table cell attachments), and `valuesData` (section field attachments) for both academic and administrative audit types, supporting `.pdf`, `.docx`, `.xlsx`, `.png`, `.jpg`, `.jpeg`, `.doc`, `.xls`, and `.zip` files.
3. **Robust Path Sanitization & Fuzzy Filename Search**:
   - Handles legacy GCP Cloud Storage URLs (`https://storage.googleapis.com/dypiu-schoolappraisal-uploads/users/...`), absolute server URLs, and relative paths (`/uploads/users/...`).
   - Strips GCP bucket name prefixes (`dypiu-schoolappraisal-uploads/`) to extract clean object names starting from `users/...`.
   - If direct path resolution fails, `LocalFileStorageService.downloadFile` executes a multi-candidate fuzzy filename search across `/app/uploads`. Normalization strips UUID prefixes and punctuation differences (spaces, underscores, hyphens) to guarantee 100% file retrieval across all school folders (`SOEMR`, `SOD`, `SOBB`, `SOCE`, `AO`, etc.).
4. **Ownership Verification for Deletes**: When a user attempts to delete an attachment (via `deleteFile`), the service extracts the object name from the URL, computes the current user's key hash, and verifies that the file prefix matches `users/<currentUserKey_hash>/attachments/`. If it does not match, it throws `You can only delete your own uploaded files.` to prevent unauthorized deletions.
5. **Dynamic URL Resolution for VM/Local Deployments**: 
   When running the application with `GCP_ENABLED: false` (e.g., on a local VM), database records migrated from GCP will still contain absolute GCS URLs starting with `https://storage.googleapis.com/...`. 
   To handle this seamlessly without manual SQL updates or affecting GCP production, the backend intercepts Jackson serialization at the JPA level inside the `Submission` and `Snapshot` models using custom getters. 
   - These getters route their JSON values (`valuesData`, `tablesData`, and `attachments`) through the `UrlPostProcessor` utility.
   - If `app.gcp.enabled` is `false`, any absolute Google Cloud Storage URLs are dynamically converted to local relative paths (e.g., `/uploads/users/...`) before being returned to the client.
   - If `app.gcp.enabled` is `true` (GCP production), the URLs are returned completely unmodified.

---

## 3. Relational Child Table Services

To support rich tabular audits, each of the 64 relational child tables maps to its own Spring Boot service (e.g. `AlumniInteractionsService`, `BestPracticesService`):
- `getBySubmissionId(Long submissionId)`: Retrieves all rows mapped to a submission.
- `saveAll(Long submissionId, List<T> rows)`: Executed inside a transactional block (`@Transactional`). Wipes all existing relational rows for the submission from the table, assigns the new list to the target `submissionId`, sets the ID of each row to `null` to trigger database inserts, and saves the new list batch in a single transactional unit.
- `deleteBySubmissionId(Long submissionId)`: Wipes all relational rows matching the submission.

