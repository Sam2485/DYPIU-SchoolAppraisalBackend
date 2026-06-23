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
    "role": "director"
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
- **Method**: `POST`
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
- **Method**: `POST`
- **Request Body**: Same as Save Draft.
- **Response (200 OK)**: Updated submission object with status `SUBMITTED`.

### Get All Submissions (Reviewers only)
- **URL**: `/all`
- **Method**: `GET`
- **Authorization**: Authorized for `ROLE_VICE-CHANCELLOR` and `ROLE_IQAC` roles.
- **Response (200 OK)**: List of all submitted/reviewed forms.

### Get Submission by ID
- **URL**: `/{id}`
- **Method**: `GET`
- **Response (200 OK)**: Submission details if owner or reviewer.

### Review Submission (Reviewers only)
- **URL**: `/{id}/review`
- **Method**: `POST`
- **Request Body**:
  ```json
  {
    "status": "APPROVED", // APPROVED, SENT_BACK, UNDER_REVIEW
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

---

## 4. Relational Child Audit Tables API (64 Tables)
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
