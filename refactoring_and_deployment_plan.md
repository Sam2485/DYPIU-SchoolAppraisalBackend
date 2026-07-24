# Refactoring and Deployment Plan: GCP & Linux VM Dual Environment Support

This document details the refactoring, configuration, and migration strategies implemented to enable the production-grade Java Spring Boot backend to support both **Google Cloud Platform (GCP)** and a **Linux Virtual Machine (VM)** environment simultaneously, using the same unified codebase.

---

## 1. Modified and New Components

### A. Modified Files
* **[AttachmentService.java](file:///C:/Users/samar/OneDrive/Desktop/Faculty%20Appraisal%20Project/DirectorAppraisal/director-appraisal/src/main/java/com/director_appraisal/director_appraisal/service/AttachmentService.java)**:
  * Refactored to delegate all upload, deletion, and stream retrieval calls to the abstract `StorageService` interface.
  * Completely eliminated all imports and references to Google Cloud Storage client libraries (`com.google.cloud.storage.*`), making it fully environment-agnostic.
  * Preserved static inner class `AttachmentResponse` and method signatures for complete backward compatibility.
* **[SecurityConfig.java](file:///C:/Users/samar/OneDrive/Desktop/Faculty%20Appraisal%20Project/DirectorAppraisal/director-appraisal/src/main/java/com/director_appraisal/director_appraisal/config/SecurityConfig.java)**:
  * Made CORS allowed origins dynamically configurable through the `app.security.cors.allowed-origins` property.
  * Cleanly injected origins using `@Value` with a safe, backward-compatible default fallback matching the hardcoded production patterns.
* **[application.yaml](file:///C:/Users/samar/OneDrive/Desktop/Faculty%20Appraisal%20Project/DirectorAppraisal/director-appraisal/src/main/resources/application.yaml)**:
  * Added `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:gcp}` to configure a default environment of `gcp` if none is explicitly specified.

### B. New Classes Created
* **[StorageService.java](file:///C:/Users/samar/OneDrive/Desktop/Faculty%20Appraisal%20Project/DirectorAppraisal/director-appraisal/src/main/java/com/director_appraisal/director_appraisal/service/StorageService.java)**:
  * A new interface defining standard file storage operations (`storeFile`, `deleteFile`, `downloadFile`).
* **[GoogleCloudStorageService.java](file:///C:/Users/samar/OneDrive/Desktop/Faculty%20Appraisal%20Project/DirectorAppraisal/director-appraisal/src/main/java/com/director_appraisal/director_appraisal/service/GoogleCloudStorageService.java)**:
  * Implements `StorageService` using GCP Cloud Storage client libraries.
  * Marked with `@Profile("gcp")` so it is only instantiated when deploying on GCP.
* **[LocalFileStorageService.java](file:///C:/Users/samar/OneDrive/Desktop/Faculty%20Appraisal%20Project/DirectorAppraisal/director-appraisal/src/main/java/com/director_appraisal/director_appraisal/service/LocalFileStorageService.java)**:
  * Implements `StorageService` using local filesystem APIs.
  * Includes built-in protection against Directory Traversal attacks.
  * Marked with `@Profile({"vm", "default"})` to be used for VM deployment and local development.

### C. Added Configuration Files
* **[application-gcp.yaml](file:///C:/Users/samar/OneDrive/Desktop/Faculty%20Appraisal%20Project/DirectorAppraisal/director-appraisal/src/main/resources/application-gcp.yaml)**:
  * Explicitly enables GCP settings and configures GCS storage dependencies.
* **[application-vm.yaml](file:///C:/Users/samar/OneDrive/Desktop/Faculty%20Appraisal%20Project/DirectorAppraisal/director-appraisal/src/main/resources/application-vm.yaml)**:
  * Configures local database defaults and sets the local storage directory (defaulting to `/opt/myapp/uploads`).
* **[logback-spring.xml](file:///C:/Users/samar/OneDrive/Desktop/Faculty%20Appraisal%20Project/DirectorAppraisal/director-appraisal/src/main/resources/logback-spring.xml)**:
  * Setup to output log files locally on the VM with a rolling history policy (maximum 10MB per file, 30 days retention, max 1GB total size) while outputting straight to Console on GCP.

---

## 2. Optional Dependencies & GCP Assumptions

### Optional Dependencies
The dependency `postgres-socket-factory` and `google-cloud-storage` inside `pom.xml` remain in the build artifact for both environments (maintaining a single artifact version). However, **their initialization is entirely optional**:
* On the VM profile (`vm`), no classes from these dependencies are instantiated or checked. The JVM only loads class definitions from the classpath if they are referenced.
* Because `GoogleCloudStorageService` is conditional on the `gcp` profile, no Google Cloud storage clients or credential managers are run in the VM context.

---

## 3. Environment Variables Reference

| Variable Name | Purpose | Default / Suggested Value (VM) |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `gcp` (fallback) or `vm` |
| `DATABASE_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://localhost:5432/director_appraisal` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `PORT` | HTTP Server port | `8080` |
| `JWT_SECRET` | Secret key for JWT signature | Must be a secure 256-bit string |
| `JWT_EXPIRATION_MS` | JWT expiration duration in MS | `86400000` (24 Hours) |
| `SMTP_HOST` | Mail server SMTP Host | `smtp.gmail.com` |
| `SMTP_PORT` | Mail server SMTP Port | `587` |
| `SMTP_USER` | SMTP username | `your-email@gmail.com` |
| `SMTP_PASSWORD` | SMTP password / App password | `your-app-password` |
| `MAIL_FROM` | Outgoing email from address | `your-email@gmail.com` |
| `LOCAL_STORAGE_DIR` | Directory for local file uploads | `/opt/myapp/uploads` |
| `BACKUP_PATH` | Directory for local backup ZIP & SQL storage | `/opt/myapp/backups` |
| `CORS_ALLOWED_ORIGINS`| Allowed CORS domains (comma-separated)| `http://localhost:5173` |

---

## 4. Suggested Project Structure

```
director-appraisal/
├── src/main/java/com/director_appraisal/director_appraisal/
│   ├── config/
│   │   ├── SecurityConfig.java         (Configurable CORS Origins)
│   │   ├── WebMvcConfig.java           (Maps local file resources)
│   │   └── JwtRequestFilter.java
│   ├── service/
│   │   ├── StorageService.java         (New Abstraction Interface)
│   │   ├── GoogleCloudStorageService.java (New GCS Implementation)
│   │   ├── LocalFileStorageService.java   (New Local Implementation)
│   │   └── AttachmentService.java      (Decoupled client wrapper)
│   └── DirectorAppraisalApplication.java
└── src/main/resources/
    ├── application.yaml                (Active profile defaulting to vm)
    ├── application-gcp.yaml            (GCP configuration)
    ├── application-vm.yaml             (VM configuration)
    ├── logback-spring.xml              (Rolling log file for VM)
    └── db/migration/                   (Unchanged database migrations)
```

---

## 5. Zero-Downtime Migration & Rollback Plans

### Zero-Downtime Migration Plan
1. **Prepare VM Environment**: Configure the PostgreSQL database and create the target upload folder on the Linux VM. Ensure firewall settings permit incoming HTTP traffic to the reverse proxy (Nginx).
2. **Build and Test Jar**: Compile the single production JAR using Maven.
3. **Double Deploy**: Start the service on the Linux VM. Keep the GCP Cloud Run deployment fully active.
4. **Data Sync**: Sync any existing files from the GCP bucket to `/opt/myapp/uploads` on the VM, preserving the paths (e.g. `users/...`).
5. **DNS Cutover**: Update the DNS record or the API routing endpoint (e.g. via Cloudflare or Nginx) to route traffic to the Linux VM.
6. **Graceful GCP Shutdown**: Keep GCP Cloud Run active for 24-48 hours to handle cached DNS clients before stopping the instances.

### Rollback Plan
If issues occur on the VM deployment (e.g., performance issues, network outages, database connection limits):
1. **Immediate DNS Reversion**: Point the DNS records or API router back to the GCP Cloud Run instance URL.
2. **Verify GCP Status**: Ensure traffic is flowing correctly to the Cloud Run services.
3. **Investigate Logs**: Inspect `/var/log/director-appraisal/app.log` or the systemd service logs on the VM to diagnose the failure without impacting production users.

---

## 6. Linux VM Deployment Instructions

### A. Pre-requisites
1. **Java Runtime**: Install OpenJDK 21.
   ```bash
   sudo apt update
   sudo apt install openjdk-21-jre-headless -y
   ```
2. **PostgreSQL**: Install and configure database.
   ```bash
   sudo apt install postgresql postgresql-contrib -y
   sudo systemctl start postgresql
   sudo systemctl enable postgresql
   ```
3. **Create Database & User**:
   ```bash
   sudo -i -u postgres psql -c "CREATE DATABASE director_appraisal;"
   sudo -i -u postgres psql -c "CREATE USER postgres WITH PASSWORD 'securepassword';"
   sudo -i -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE director_appraisal TO postgres;"
   ```
4. **Upload Directory Setup**:
   ```bash
   sudo mkdir -p /opt/myapp/uploads
   sudo chown -R ubuntu:ubuntu /opt/myapp/uploads
   ```

### B. Setup Systemd Service
Create a service file at `/etc/systemd/system/director-appraisal.service`:
```ini
[Unit]
Description=Director Appraisal Backend
After=syslog.target network.target postgresql.service

[Service]
User=ubuntu
WorkingDirectory=/opt/myapp
ExecStart=/usr/bin/java -jar director-appraisal-0.0.1-SNAPSHOT.jar
SuccessExitStatus=143

# Environment Variables Configuration
Environment=SPRING_PROFILES_ACTIVE=vm
Environment=DATABASE_URL=jdbc:postgresql://localhost:5432/director_appraisal
Environment=DB_USERNAME=postgres
Environment=DB_PASSWORD=securepassword
Environment=LOCAL_STORAGE_DIR=/opt/myapp/uploads
Environment=PORT=8080
Environment=JWT_SECRET=YourSuperSecretKeyHereChangeMeToSomethingLong
Environment=CORS_ALLOWED_ORIGINS=http://localhost:5173,https://your-domain.com

Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```
Enable and start the service:
```bash
sudo systemctl daemon-reload
sudo systemctl enable director-appraisal.service
sudo systemctl start director-appraisal.service
```

### C. Nginx Configuration (Optional but recommended)
Setup Nginx to reverse proxy traffic to `http://127.0.0.1:8080` and serve the local `/uploads/**` static path directly for maximum performance:
```nginx
# Nginx configuration for School Appraisal Dashboard (school_appraisal)
# Place this inside: /etc/nginx/sites-available/school_appraisal

# 1. BACKEND API PROXY (Port 8001)
server {
    listen 8001;
    server_name 10.100.0.23;

    # Allow unlimited file size for uploads backups
    client_max_body_size 0;

    # Serve uploads directly via Nginx alias
    location /uploads/ {
        alias /home/dypiu/DYPIU-SchoolAppraisalBackend/uploads/;
        expires 30d;
        access_log off;
        add_header Cache-Control "public, no-transform";
    }

    # Proxy all API requests to Spring Boot
    location / {
        proxy_pass http://127.0.0.1:8080; # Spring Boot running locally
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# 2. FRONTEND CLIENT SERVER (Port 3001)
server {
    listen 3001;
    server_name 10.100.0.23;

    # Allow unlimited file size for uploads backups on port 3001 as well
    client_max_body_size 0;

    # Proxy API requests to backend if they are sent to port 3001
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Serve built static files directly from dist folder
    location / {
        root /home/dypiu/DYPIU-SchoolAppraisal-frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # Optional: Proxy to Vite dev server (uncomment if running npm run dev)
    # location / {
    #     proxy_pass http://127.0.0.1:5173;
    #     proxy_set_header Host $host;
    #     proxy_set_header X-Real-IP $remote_addr;
    # }
}

---

## 7. Multi-Auditor Review Workflow (Administrative Audits)

### A. Core Architecture
The system supports multiple assigned auditors (Internal and External) reviewing the same administrative submission concurrently. The lifecycle is managed via:
1. **`SubmissionAuditorAssignment` Entity**: Maps a specific auditor (identified by `auditorId`/`auditorEmail`) and auditor type (`INTERNAL` or `EXTERNAL`) to a `Submission`.
2. **Dynamic Completion Routing**: The overall submission status changes to `AUDITOR_COMPLETED` only when all assignments matching the active forwarded auditor type are in a `SUBMITTED` state.

### B. Ownership Resolution
To resolve which review remark data and draft values belong to the active logged-in auditor:
* **First Match**: Explicit assignments matching `auditorId` or `auditorEmail`.
* **Legacy Mapped Post Fallback**: Used only for legacy submissions lacking structured auditor assignments where matching is resolved via the auditor's administrative post.

### C. Backend API Handlers
* **`submitAuditorReview`**: Saves remarks, attachments, and transitions status. Recomputes progress across active assignments.
* **`reviewSubmission` (IQAC Approval)**: Dynamically scans the database to verify if all auditor assignments for the active cycle have been completed before permitting approval.

```
