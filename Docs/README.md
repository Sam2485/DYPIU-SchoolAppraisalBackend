# School Appraisal Backend Documentation

Welcome to the documentation for the School Appraisal Java Spring Boot Backend. This project is a Spring Boot-based REST API designed to manage the appraisal lifecycle for Directors (Academic branch) and Administrative Users (Registrar, HR, DSW, Placement), with review capabilities for the Vice-Chancellor (VC) and the Internal Quality Assurance Cell (IQAC).

---

## 📖 Table of Contents

1. [API Endpoints](API_ENDPOINTS.md) - Detailed catalog of all REST API routes, requests, and response payloads.
2. [Database Schema](DATABASE_SCHEMA.md) - Database entity design, child tables, and Flyway migrations setup.
3. [Security & Authorization](SECURITY.md) - JWT authentication framework, CORS configurations, and role-based access rules.
4. [Service Layer Architecture](SERVICE_LAYER.md) - Explanation of the business logic services, file upload engines, and storage workflows.

---

## 🛠️ Tech Stack & Requirements

- **Java Development Kit (JDK)**: Version 21
- **Build Tool**: Maven 3.x
- **Framework**: Spring Boot 3.3.4
- **Security**: Spring Security 6.x (JWT-based stateless authentication)
- **Database**: PostgreSQL (GCP Cloud SQL or local database)
- **Database Migrations**: Flyway Core & Flyway Database PostgreSQL
- **Object Storage**: Google Cloud Storage (with local disk backup storage fallback)
- **Mailing**: JavaMailSender (integrated with SMTP for password resets)

---

## ⚙️ Environment Variables

The application can be configured dynamically using the following environment variables:

| Variable | Description | Default Value |
| :--- | :--- | :--- |
| `PORT` | The port on which the Spring Boot application runs. | `8080` |
| `DATABASE_URL` | PostgreSQL connection URL. | `jdbc:postgresql://localhost:5432/director_appraisal` |
| `DB_USERNAME` | Database connection username. | `postgres` |
| `DB_PASSWORD` | Database connection password. | `postgres` |
| `JWT_SECRET` | Secret key used for signing JWT tokens (HMAC-SHA256). | `SecretKeyForJWTAppraisalMustBeAtLeast256Bits...` |
| `GCP_ENABLED` | Set `true` to enable Google Cloud Storage for attachments. | `false` |
| `GCP_BUCKET_NAME` | GCP Cloud Storage bucket name. | `director-appraisal-attachments` |
| `GCP_PROJECT_ID` | GCP Project ID. | `director-appraisal-project` |
| `LOCAL_STORAGE_DIR` | Local disk folder for file upload fallback. | `./uploads` |
| `FRONTEND_URL` | URL of the frontend application for reset links. | `http://localhost:5173` |
| `SMTP_HOST` | Host of SMTP email server for password resets. | `smtp.gmail.com` |
| `SMTP_PORT` | Port of SMTP email server. | `587` |
| `SMTP_USER` | Username/email of the sending mail account. | `your-email@gmail.com` |
| `SMTP_PASSWORD` | App password/credential of the sending mail account. | `your-app-password` |

---

## 🚀 Local Development Setup

To run this application locally, follow these steps:

### 1. Database Setup
1. Make sure **PostgreSQL** is installed and running on port 5432.
2. Log in to your PostgreSQL console and create a new database:
   ```sql
   CREATE DATABASE director_appraisal;
   ```

### 2. Run the Application
You can run the application directly using Maven. Make sure to define any necessary environment variables if you deviate from the default configuration:
```bash
mvn spring-boot:run
```

During startup, **Flyway** will detect the database and run the baseline migration script (`src/main/resources/db/migration/V1__init_schema.sql`), creating the `users`, `submissions`, `snapshots`, `password_reset_tokens` tables, and all 64 relational child tables.

### 3. File Upload Mode
- By default, the application runs with `USE_LOCAL_STORAGE: true` and `GCP_ENABLED: false`.
- Any file uploaded will go into the `./uploads/` directory inside the project root directory.
- Mapped URLs will look like: `/uploads/users/<user_hash>/attachments/<file_hash>.pdf`.

---

## 🐋 Docker & Containerization

A multi-stage `Dockerfile` is provided for containerizing the application:

1. **Stage 1 (Build)**: Builds a thin fat-JAR using `maven:3.9.8-eclipse-temurin-21-alpine`.
2. **Stage 2 (Runtime)**: Runs the JAR inside `eclipse-temurin:21-jre-alpine` under a secure, non-privileged `spring` user.

To build the Docker image locally:
```bash
docker build -t director-appraisal-backend .
```

To run the container locally:
```bash
docker run -p 8080:8080 --env DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/director_appraisal --env DB_USERNAME=postgres --env DB_PASSWORD=postgres director-appraisal-backend
```

---

## ☁️ Google Cloud Deployment (CI/CD)

The project includes a `cloudbuild.yaml` file for automated deployment to **Google Cloud Run**:

1. **Trigger**: Google Cloud Build compiles the code and builds the Docker image.
2. **Image Registry**: The image is pushed to **GCP Artifact Registry** (default region `asia-south1`).
3. **Deployment**: Deploys to **Google Cloud Run** under `director-appraisal-backend`.
4. **Resources Integration**:
   - Attaches a **GCP Cloud SQL** (PostgreSQL) database instance using Socket Factory.
   - Inject environment variables, secrets (from GCP Secret Manager), and enables GCP Cloud Storage for attachments.

---

## 📂 Project Structure

```
director-appraisal/
├── Docs/                     # Documentation files
├── src/
│   ├── main/
│   │   ├── java/com/director_appraisal/director_appraisal/
│   │   │   ├── config/      # MVC, JWT, Security filters & setups
│   │   │   ├── controller/  # REST endpoints (Academic & Administrative sub-packages)
│   │   │   ├── exception/   # Custom global exception handler structures
│   │   │   ├── model/       # JPA entities (Academic & Administrative sub-packages)
│   │   │   ├── repository/  # JPA Spring Data Repositories (Academic & Administrative)
│   │   │   └── service/     # Business logic layer (Academic & Administrative services)
│   │   └── resources/
│   │       ├── db/migration/ # Flyway SQL migration scripts (V1__init_schema.sql)
│   │       └── application.yaml # Standard configurations and profiles
│   └── test/
│       └── java/.../        # JUnit unit and context tests
├── Dockerfile                # Production multi-stage build manifest
├── cloudbuild.yaml           # Google Cloud Build deployment script
└── pom.xml                   # Maven dependencies and build targets
```

