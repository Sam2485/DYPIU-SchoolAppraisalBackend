# Service Layer & Storage Workflows

This document describes the business services and file storage logic implemented in the School Appraisal Backend.

---

## 1. Core Services

### UserService
Manages user lifecycle and authorization details.
- `loadUserByUsername`: Dynamically loads a user from the database by email (integrates with Spring Security).
- `createUser`: Hashes password credentials securely via `BCrypt` and writes profiles.
- `checkPassword`: Matches raw login inputs against database-encrypted keys.

### SubmissionService
Implements form flow logic, draft overrides, and historical snapshot records:
- `getOrCreateDraft`: Retrieves the current active draft for a submitter. If none exists, initializes and saves a new blank draft record.
- `saveDraft`: Overwrites current field values and table JSON bodies, increments document version, and calls `createSnapshot`.
- `submitForm`: Set status to `SUBMITTED`, logs timestamp, increments version, and triggers `createSnapshot`.
- `reviewSubmission`: Allows VC/IQAC to update form status (`APPROVED`, `SENT_BACK`) and logs reviewer remarks.
- `getSnapshotsForSubmission`: Retrieves history snapshot versions.

---

## 2. File Upload Engine (GCP & Local Fallback)

To support rules requiring PDF attachments (max 10MB) for database audits, **AttachmentService** implements an adaptive dual-storage workflow:

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
               │   GCP Storage Initialized│
               └──────┬────────────┬──────┘
                      │            │
             (Yes) ───┘            └─── (No)
              ▼                         ▼
┌──────────────────────────┐   ┌──────────────────────────┐
│ Upload to GCP bucket     │   │ Save to local disk path  │
│ Return storage URL       │   │ Return relative local URL│
└──────────────────────────┘   └──────────────────────────┘
```

- **GCP Storage (Production Mode)**: File is uploaded to the Google Cloud Storage bucket specified in the configurations.
- **Local Fallback (Development Mode)**: File is stored locally under `/uploads/` and served via static resource handlers.

---

## 3. Relational Child Table Services

To support relational tables mapping (64 tables), a generic transaction-based Service pattern is implemented for each child entity (e.g. `StudentStrengthService`, `BoardOfStudiesService`):
- `getBySubmissionId`: Returns all relational rows for a given form submission.
- `saveAll`: Atomically wipes stale records for a submission and re-saves the new batch list in a single transactional block (`@Transactional`).
