# Service Layer & Storage Workflows

This document describes the business services, transactional processes, and file storage workflows implemented in the School Appraisal Backend.

---

## 1. Core Services

### UserService
Integrates with Spring Security's `UserDetailsService` and handles user authentication, profile CRUD operations, and password reset flows:
- `loadUserByUsername(String email)`: Dynamically retrieves a user from the database by email address.
- `findByEmail(String email)`: Checks for user existence by email.
- `findById(Long id)`: Fetches a user profile by database primary key.
- `findAllUsers()`: Retrieves a list of all user profiles registered in the system.
- `createUser(User user)`: Checks for email conflicts, encodes the raw password using BCrypt hashing, and persists the new user.
- `updateUser(User user, String rawPassword)`: Saves changes to name, email, role, school, and designation. If a non-blank `rawPassword` is supplied, it re-hashes and updates the user's password.
- `deleteUser(User user)`: Removes the user profile from the database.
- `checkPassword(String rawPassword, String encodedPassword)`: Compares raw login inputs against BCrypt-encrypted database passwords.
- `createPasswordResetToken(String email)`: Performs password reset token generation:
  1. Locates the user or throws an exception.
  2. Wipes any stale reset tokens for the user's email.
  3. Generates a secure raw token via UUID.
  4. Hashes the raw token with SHA-256 to save in the database (protects token database leakage).
  5. Formulates a reset URL using the configured `FRONTEND_URL` and sends it to the user's email via `EmailService`.
- `resetPassword(String rawToken, String newPassword)`: Hashes the raw token from the link, queries the database, verifies that the token is valid, active, and unused, updates the user's password with BCrypt, and flags the reset token as used.

### EmailService
Wraps Spring Boot's `JavaMailSender` to send simple transactional text emails.

### SubmissionService
Implements the core appraisal form lifecycle, draft management, and version/history snapshot capturing:
- `getOrCreateDraft(String email, String auditType)`: Retrieves the currently active draft for the submitter. If none exists, builds and saves a new blank submission record with status `DRAFT`.
- `saveDraft(String email, ...)`: Updates draft values, increments the document version counter, and triggers `createSnapshot`. Throws an error if the form has already been `APPROVED`.
- `submitForm(String email, ...)`: Updates values, overrides status to `SUBMITTED`, logs the `submittedAt` timestamp, increments the version counter, and triggers `createSnapshot`.
- `updateSubmissionById(Long id, ...)`: Updates submission values by ID. Validates that the caller is the owner and that the submission is not already `APPROVED`.
- `reviewSubmission(Long id, String status, String remarks, String reviewerName)`: Invoked by VC/IQAC reviewers to set the final status (`APPROVED`, `SENT_BACK`, `UNDER_REVIEW`), logs reviewer remarks, and triggers `createSnapshot`.
- `getSnapshotsForSubmission(Long submissionId)`: Retrieves historical snapshots, ordered from newest to oldest.
- `createSnapshot(Submission submission)`: Prepares and writes a historical record to the `snapshots` table every time the submission state is saved, submitted, or reviewed.

---

## 2. File Upload Engine (GCP & Local Fallback)

The **AttachmentService** implements an adaptive dual-storage workflow for processing PDF attachments (max 10MB):

```
                   PDF File Upload Request
                             │
                             ▼
                ┌──────────────────────────┐
                │    Validates File Type   │ <── Verifies extension is .pdf
                └────────────┬─────────────┘
                             │
                             ▼
                ┌──────────────────────────┐
                │    Validates File Size   │ <── Enforces maximum size of 10MB
                └────────────┬─────────────┘
                             │
                             ▼
                ┌──────────────────────────┐
                │   Computes Content Hash  │ <── SHA-256 hash of byte contents
                └────────────┬─────────────┘
                             │
                             ▼
                ┌──────────────────────────┐
                │   Verifies File Duplicate│ <── Rejects if uploader already has hash
                └────────────┬─────────────┘
                             │
                             ▼
                ┌──────────────────────────┐
                │   GCP Storage Enabled?   │
                └──────┬────────────┬──────┘
                       │            │
              (Yes) ───┘            └─── (No / Fail)
               ▼                         ▼
 ┌──────────────────────────┐   ┌──────────────────────────┐
 │ Upload to GCP bucket     │   │ Save to local disk path  │
 │ Return Google Cloud URL  │   │ Return relative local URL│
 └──────────────────────────┘   └──────────────────────────┘
```

### Key Upload Mechanics:
1. **Deduplication**: On upload, the service calculates the SHA-256 checksum of the file bytes. It builds the upload path as:
   `users/<userKey_hash>/attachments/<content_sha256>.pdf`
   If the file already exists at this path, the service rejects the request with an error to prevent duplicate storage.
2. **Multiple Upload support**: `uploadFiles` handles bulk uploads of files. It verifies that all files are PDFs and do not exceed the 10MB limit, then processes them sequentially.
3. **Ownership Verification for Deletes**: When a user attempts to delete an attachment (via `deleteFile`), the service extracts the object name from the URL, computes the current user's key hash, and verifies that the file prefix matches `users/<currentUserKey_hash>/attachments/`. If it does not match, it throws `You can only delete your own uploaded files.` to prevent unauthorized deletions.

---

## 3. Relational Child Table Services

To support rich tabular audits, each of the 64 relational child tables maps to its own Spring Boot service (e.g. `AlumniInteractionsService`, `BestPracticesService`):
- `getBySubmissionId(Long submissionId)`: Retrieves all rows mapped to a submission.
- `saveAll(Long submissionId, List<T> rows)`: Executed inside a transactional block (`@Transactional`). Wipes all existing relational rows for the submission from the table, assigns the new list to the target `submissionId`, sets the ID of each row to `null` to trigger database inserts, and saves the new list batch in a single transactional unit.
- `deleteBySubmissionId(Long submissionId)`: Wipes all relational rows matching the submission.

