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

### Get All Submissions (Reviewers only)
- **URL**: `/all`
- **Method**: `GET`
- **Authorization**: Authorized for `ROLE_VICE-CHANCELLOR` and `ROLE_IQAC` roles.
- **Response (200 OK)**: List of all submitted/reviewed forms.

### Get / Update Submission by ID
- **URL**: `/{id}`
- **Method**: `GET` | `PUT`
- **Request Body (For PUT)**: Same as Save Draft.
- **Response (200 OK)**: Submission details or updated submission object if owner (or reviewer for GET).

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
- **Request Body**:
  ```json
  {
    "category": "academic", // "academic" or "administrative"
    "role": "director",      // must match category ("director" for academic, "administrative" for administrative)
    "school": "School of Computer Science & Applications", // Selected School (or "Administrative Office" for admin category)
    "designation": "Director", // Optional custom designation (if empty, matches defaults/posts)
    "post": null, // Required if category is "administrative": "registrar", "hr", "dean-student-welfare", "dean-placement"
    "name": "Full Name",
    "email": "user@dypiu.ac.in",
    "password": "Password123" // Minimum 6 characters
  }
  ```
- **Response (201 Created)**:
  ```json
  {
    "message": "User created successfully",
    "user": {
      "id": 4,
      "name": "Full Name",
      "email": "user@dypiu.ac.in",
      "category": "academic",
      "role": "director",
      "school": "School of Computer Science & Applications",
      "designation": "Director",
      "post": null,
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
