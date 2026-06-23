# School Appraisal Backend Documentation

Welcome to the documentation for the School Appraisal Java Spring Boot Backend. This project is a Spring Boot-based REST API designed to manage the appraisal lifecycle for Directors (Academic branch) and Administrative Users (Registrar, HR, DSW, Placement), with review capabilities for VC and IQAC.

## Table of Contents

1. [API Endpoints](API_ENDPOINTS.md) - Detailed catalog of all REST API routes, requests, and response payloads.
2. [Database Schema](DATABASE_SCHEMA.md) - Database entity design and Flyway migrations setup.
3. [Security & Authorization](SECURITY.md) - JWT authentication framework, cors configurations, and role-based access rules.
4. [Service Layer Architecture](SERVICE_LAYER.md) - Explanation of the business logic services and storage engine workflows.

## Project Structure

The project code is modularized as follows:

- `src/main/java/com/director_appraisal/director_appraisal/`
  - `config/`: System configurations (CORS, MVC, Security, Seeding).
  - `controller/`: REST controllers, divided into `academic/` and `administrative/` folders.
  - `exception/`: Global Exception Handler mapping exceptions to clear network JSON payloads.
  - `model/`: JPA database entity models, divided into `academic/` and `administrative/` folders.
  - `repository/`: JPA repositories, divided into `academic/` and `administrative/` folders.
  - `service/`: Transactional services, divided into `academic/` and `administrative/` folders.

- `src/main/resources/`
  - `application.yaml`: Configuration profiles for PostgreSQL, Flyway, JWT, and GCP.
  - `db/migration/`: Flyway baseline database migration scripts.
