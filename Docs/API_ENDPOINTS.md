# API Endpoints Documentation

This document catalogs all the REST API endpoints exposed by the School Appraisal System.

---

## 1. Authentication Module
**Base Path**: `/api/auth`

### User Login
- **URL**: `/login`
- **Method**: `POST`
- **Headers**: `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "username": "director@dypiu.ac.in",
    "password": "Director@123"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "email": "director@dypiu.ac.in",
    "name": "Director of Schools",
    "designation": "Director",
    "school": "School of Computer Science Engg. & App.",
    "role": "director",
    "id": 2,
    "userId": 2,
    "accountType": "user",
    "category": "academic",
    "auditorType": null,
    "auditorRole": null,
    "post": null
  }
  ```

### Forgot Password (Request Reset Link)
- **URL**: `/forgot-password`
- **Method**: `POST`
- **Request Body**:
  ```json
  {
    "email": "director@dypiu.ac.in"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "message": "If that email is registered, a reset link has been generated.",
    "token": "d748f2a1b18c474eb44747a00f2ad37e" // Hashed token details are sent to the registered email address
  }
  ```

### Reset Password (Submit New Password)
- **URL**: `/reset-password`
- **Method**: `POST`
- **Request Body**:
  ```json
  {
    "token": "d748f2a1b18c474eb44747a00f2ad37e",
    "newPassword": "newSecretPassword123"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "message": "Password has been reset successfully."
  }
  ```

---

## 2. Appraisal Submissions Module
**Base Path**: `/api/submissions`
**Headers**: `Authorization: Bearer <jwt-token>`

### Get Submitter's Draft
- **URL**: `/my-draft`
- **Method**: `GET`
- **Query Params**: `auditType` (`academic` or `administrative`)
- **Response (200 OK)**:
  ```json
  {
    "id": 1,
    "email": "director@dypiu.ac.in",
    "auditType": "academic",
    "school": "School of Computer Science Engg. & App.",
    "submittedBy": "Director of Schools",
    "submittedAt": null,
    "status": "DRAFT",
    "remarks": null,
    "reviewedBy": null,
    "reviewedAt": null,
    "valuesData": "{}",
    "tablesData": "{}",
    "attachments": "[]",
    "version": 1
  }
  ```

### Save Form Draft
- **URL**: `/save-draft`
- **Method**: `POST` | `PUT`
- **Request Body**:
  ```json
  {
    "auditType": "academic",
    "valuesData": "{\"schoolName\":\"SoCSEA\", ...}",
    "tablesData": "{\"studentStrength\":[...], ...}",
    "attachments": "[\"syllabus_mom.pdf\"]"
  }
  ```
- **Response (200 OK)**: Updated submission object.

### Submit Form
- **URL**: `/submit`
- **Method**: `POST` | `PUT`
- **Request Body**: Same as Save Draft.
- **Response (200 OK)**: Updated submission object with status `SUBMITTED`.

### Get All Submissions
- **URL**: `/all`
- **Method**: `GET`
- **Authorization**: Authorized for `ROLE_VICE-CHANCELLOR`, `ROLE_IQAC`, and all `auditor` roles.
- **Filtering Logic**:
  - **IQAC**: returns submitted, under review, auditor completed, and approved/sent-back forms.
  - **VC**: returns only `AUDITOR_COMPLETED` and `APPROVED` forms (blocked before audit completion).
  - **Auditors**: returns only forms where the auditor is explicitly assigned (listed in `forwardedToAuditorIds`/`forwardedToAuditorEmails`) or fallback matched (category, auditorType, and school/post match) with status `UNDER_REVIEW` or `AUDITOR_COMPLETED`.
- **Response (200 OK)**: List of submission objects.

### Get / Update Submission by ID
- **URL**: `/{id}`
- **Method**: `GET` | `PUT`
- **Access Limits**:
  - Owners can read/write drafts.
  - IQAC can read/write and configure forwarding.
  - VC can read only after `AUDITOR_COMPLETED`.
  - Auditors can read only if assigned/matched.
  - Auditors can write/update only Part E (academic) or Part F (administrative) fields and set status to `AUDITOR_COMPLETED`.
- **Request Body (For PUT forwarding by IQAC)**:
  ```json
  {
    "forwardedAuditorType": "internal", // "internal" | "external"
    "status": "UNDER_REVIEW",
    "forwardedToAuditorIds": [4, 5], // array of auditor user IDs (optional)
    "forwardedToAuditorNames": ["Auditor A", "Auditor B"], // (optional)
    "forwardedToAuditorEmails": ["auditor.a@dypiu.ac.in", "auditor.b@dypiu.ac.in"] // (optional)
  }
  ```
- **Request Body (For PUT completion by Auditor)**:
  ```json
  {
    "status": "AUDITOR_COMPLETED",
    "valuesData": "{\"__auditSignOff\":{\"auditedBy\":{...}}, ...}",
    "tablesData": "...",
    "attachments": "..."
  }
  ```
- **Response (200 OK)**: Updated submission object. When fetching a submission (either via `GET /{id}` or in the lists returned by `GET /all`), if it is an administrative audit, it dynamically includes a transient **`permissions`** property describing the caller's post-level edit capabilities:
  ```json
  {
    "id": 1,
    "email": "...",
    "auditType": "ADMINISTRATIVE",
    "status": "UNDER_REVIEW",
    "version": 1,
    "permissions": {
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
  }
  ```

### Review Submission (Reviewers only)
- **URL**: `/{id}/review`
- **Method**: `POST`
- **Access Rule**: Blocked unless submission status is already `AUDITOR_COMPLETED`.
- **Request Body**:
  ```json
  {
    "status": "APPROVED", // APPROVED, SENT_BACK
    "remarks": "Form review passed. Checked records."
  }
  ```
- **Response (200 OK)**: Updated submission object.

### Get Historical Snapshots
- **URL**: `/{id}/snapshots`
- **Method**: `GET`
- **Response (200 OK)**: List of snapshots tracking form history.

---

## 3. Attachments Module
**Base Path**: `/api/attachments`

### Upload PDF Document
- **URL**: `/upload`
- **Method**: `POST`
- **Content-Type**: `multipart/form-data`
- **Request Parameters**:
  - `file` (Multipart file, required, PDF only, max 10MB).
- **Response (200 OK)**:
  ```json
  {
    "name": "syllabus_mom.pdf",
    "url": "/uploads/3fb21884-633b-4cf7-9a4f-ea819777c9b0-syllabus_mom.pdf"
  }
  ```

### Upload Multiple PDF Documents
- **URL**: `/upload-multiple`
- **Method**: `POST`
- **Content-Type**: `multipart/form-data`
- **Request Parameters**:
  - `files` (Array of Multipart files, optional, PDF only, max 10MB each).
  - `file` (Array of Multipart files, optional, fallback parameter name).
- **Response (200 OK)**:
  ```json
  [
    {
      "name": "syllabus_mom.pdf",
      "url": "/uploads/users/5a7f9e8a/attachments/8d9c2b4a.pdf"
    },
    {
      "name": "nep_policy.pdf",
      "url": "/uploads/users/5a7f9e8a/attachments/7e8a9b2c.pdf"
    }
  ]
  ```

### Delete PDF Document
- **URL**: `/delete`
- **Method**: `DELETE`
- **Query Parameters**:
  - `url` (String, optional): The URL of the attachment to delete.
- **Request Body**: Fallback JSON if `url` is not passed as a query parameter:
  ```json
  {
    "url": "/uploads/users/5a7f9e8a/attachments/8d9c2b4a.pdf"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "message": "File deleted successfully."
  }
  ```
- **Authorization**: Uploader must be the owner of the target file (checked via uploader key prefix checking).

---

## 4. User Management Module (IQAC Users Only)
**Base Path**: `/api/users`
**Headers**: `Authorization: Bearer <jwt-token>`

This module provides full CRUD capabilities over user profiles. Access is restricted to users with the `iqac` role.

### Retrieve All Users
- **URL**: `/`
- **Method**: `GET`
- **Response (200 OK)**:
  ```json
  {
    "users": [
      {
        "id": 2,
        "name": "Academic Director SoCSEA",
        "email": "director.socsea@dypiu.ac.in",
        "category": "academic",
        "role": "director",
        "school": "School of Computer Science & Applications",
        "designation": "Director",
        "post": null,
        "status": "active"
      },
      {
        "id": 3,
        "name": "Registrar Admin",
        "email": "registrar@dypiu.ac.in",
        "category": "administrative",
        "role": "administrative",
        "school": "Administrative Office",
        "designation": "Registrar",
        "post": "registrar",
        "status": "active"
      }
    ]
  }
  ```

### Create User
- **URL**: `/`
- **Method**: `POST`
- **Request Body (For Standard Users)**:
  ```json
  {
    "category": "academic", // "academic" or "administrative"
    "role": "director",      // must match category
    "school": "School of Computer Science & Applications",
    "designation": "Director",
    "name": "Full Name",
    "email": "user@dypiu.ac.in",
    "password": "Password123"
  }
  ```
- **Request Body (For Auditors)**:
  ```json
  {
    "accountType": "auditor", // or "userType": "auditor"
    "category": "academic", // or "auditCategory" ("academic" | "administrative")
    "auditorType": "internal", // "internal" | "external"
    "auditorRole": "academic-internal-auditor", // specific auditor role
    "school": "School of Computer Science & Applications", // required for academic auditors
    "post": null, // required for administrative auditors (e.g. "registrar", "hr", "dean-student-welfare", "dean-placement")
    "name": "Auditor Name",
    "email": "auditor@dypiu.ac.in",
    "password": "Password123"
  }
  ```
- **Response (201 Created)**:
  ```json
  {
    "message": "User created successfully",
    "user": {
      "id": 4,
      "name": "Auditor Name",
      "email": "auditor@dypiu.ac.in",
      "category": "academic",
      "role": "academic-internal-auditor",
      "school": "School of Computer Science & Applications",
      "designation": "Internal Academic Auditor",
      "post": null,
      "accountType": "auditor",
      "auditorType": "internal",
      "auditorRole": "academic-internal-auditor",
      "status": "active"
    }
  }
  ```

### Update User
- **URL**: `/{id}`
- **Method**: `PUT`
- **Request Body**: Same schema as Create User. Email updates are verified for conflicts. Password is optional (updates only if not blank).
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "message": "User updated successfully",
    "user": { ... }
  }
  ```

### Delete User
- **URL**: `/{id}`
- **Method**: `DELETE`
- **Response (200 OK)**:
  ```json
  {
    "success": true,
    "message": "User deleted successfully"
  }
  ```

---

## 5. Relational Child Audit Tables API (64 Tables)
**Base Path**: `/api/tables`
**Headers**: `Authorization: Bearer <jwt-token>`

Every generated relational table exposes standard endpoints for bulk updates.
- Replace `{table-name}` with the table's database endpoint name (e.g. `student_strength`, `board_of_studies`, `courses_offered`).

### Retrieve Relational Rows for a Submission
- **URL**: `/{table-name}/submission/{submissionId}`
- **Method**: `GET`
- **Response (200 OK)**: Array of rows corresponding to the table columns.

### Save Relational Rows for a Submission
- **URL**: `/{table-name}/submission/{submissionId}`
- **Method**: `POST`
- **Request Body**: Array of row objects.
- **Response (200 OK)**: List of saved rows.

### Clear Relational Rows for a Submission
- **URL**: `/{table-name}/submission/{submissionId}`
- **Method**: `DELETE`
- **Response (200 OK)**: `200 OK` (Clear successful).
# Shared Administrative Audit

Administrative authorities use one shared form per academic year:

```http
GET /api/submissions/my-draft?auditType=administrative&shared=true
```

Partial save payload:

```json
{
  "auditType": "administrative",
  "sharedAdministrativeForm": true,
  "contributorPost": "registrar",
  "sections": ["A"],
  "valuesData": "{}",
  "tablesData": "{}",
  "attachments": "[]"
}
```

Contribution approval uses `PUT /api/submissions/{id}` with:

```json
{
  "auditType": "administrative",
  "sharedAdministrativeForm": true,
  "action": "APPROVE_CONTRIBUTION",
  "contributorPost": "registrar",
  "sections": ["A", "C"],
  "valuesData": "{}",
  "tablesData": "{}",
  "attachments": "[]"
}
```

Section ownership:

- Registrar: Part A, Part C
- HR: Part B
- Dean Student Welfare: Part D
- Dean Placement: Part E

### Get Shared Administrative Form Status
- **URL**: `/administrative/{cycleId}/status`
- **Method**: `GET`
- **Headers**: `Authorization: Bearer <jwt-token>`
- **Response (200 OK)**:
  ```json
  {
    "id": 500,
    "email": "administrative.shared@dypiu.ac.in",
    "auditType": "administrative",
    "status": "DRAFT",
    "submittedBy": {
      "registrar": {
        "submitted": true,
        "submittedAt": "2026-06-29T18:10:08",
        "name": "Registrar User",
        "email": "registrar@dypiu.ac.in"
      },
      "hr": {
        "submitted": false,
        "submittedAt": null,
        "name": null,
        "email": null
      },
      "deanStudentWelfare": {
        "submitted": false,
        "submittedAt": null,
        "name": null,
        "email": null
      },
      "deanPlacement": {
        "submitted": false,
        "submittedAt": null,
        "name": null,
        "email": null
      }
    }
  }
  ```

### Submit Administrative Role Section
- **URL**: `/administrative/{cycleId}/submit`
- **Method**: `POST`
- **Headers**: `Authorization: Bearer <jwt-token>`
- **Response (200 OK)**:
  - Returns the updated `Submission` entity. If all four roles have successfully submitted, the overall submission status transitions automatically to `SUBMITTED`.

ZIP download for all administrative contributors:

```http
GET /api/submissions/{id}/attachments/download?includeAllContributors=true
```
