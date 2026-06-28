# ⚡ Latency & Performance Optimization Guide

This document provides a comprehensive overview of latency sources in the School Appraisal Backend, how they have been optimized, and architectural recommendations for keeping API response times sub-second on **GCP Cloud Run** and **PostgreSQL (Cloud SQL)**.

---

## 📖 Table of Contents
1. [Database Latency (Solved via Indexing)](#1-database-latency-solved-via-indexing)
2. [GCP Cloud Run Cold Start Latency](#2-gcp-cloud-run-cold-start-latency)
3. [Spring Data JPA & Transaction Tuning](#3-spring-data-jpa--transaction-tuning)
4. [HikariCP Connection Pool Latency](#4-hikaricp-connection-pool-latency)
5. [Recommended GCP Deployment Flags](#5-recommended-gcp-deployment-flags)

---

## 1. Database Latency (Solved via Indexing)

### The Problem
Appraisal submissions accumulate structured data across 64 normalization tables. The master `submissions` table is frequently searched by:
* User email address
* Active academic year
* Audit status (e.g. for VC or IQAC dashboards)
* Version lineage (parent/root IDs)

Without indexes, PostgreSQL must perform **Sequential Scans** ($O(N)$ complexity) on every query. As the number of users and submissions scales, query latencies increase exponentially.

### The Solution: V9 Flyway Indexes
In `V9__add_indexes.sql`, we added selective B-tree indexes targeting the exact query patterns in `SubmissionRepository.java`:
1. **`idx_submissions_email_audit_type_year`**: A composite index on `(email, audit_type, academic_year)`. Speed up lookup for checking existing drafts, saving drafts, and pulling submitter histories.
2. **`idx_submissions_status`**: B-tree index on `status`. Speeds up VC and IQAC dashboards which query submissions by status filters.
3. **`idx_submissions_root_parent_id`**: Composite index on `(root_submission_id, parent_submission_id)`. Drastically reduces time to build version lineage history.
4. **`idx_users_role` & `idx_users_status`**: Optimizes role verification scans during request filtering.

---

## 2. GCP Cloud Run Cold Start Latency

Java Spring Boot applications are prone to high **Cold Start** latency (up to 15 seconds) when a container instance spins up from zero. This is due to JVM class-loading, classpath scanning, and Hibernate/Flyway context initialization.

### Current Optimizations in `Dockerfile`
Our multi-stage container build applies several key JVM switches to reduce startup latency:
* **`-XX:+UseContainerSupport`**: Prevents container miscalculation of CPU/RAM limits.
* **`-XX:MaxRAMPercentage=75.0` & `-XX:InitialRAMPercentage=50.0`**: Prevents the JVM from slowly growing its heap dynamically on startup, which incurs significant system page fault overhead.
* **`-Dspring.jmx.enabled=false`**: Disables Java Management Extensions (JMX), saving around 1–1.5 seconds of context boot time.
* **`-Djava.security.egd=file:/dev/./urandom`**: Configures the JVM to use a non-blocking random entropy source, preventing startup blocks when securing JWT keys.

---

## 3. Spring Data JPA & Transaction Tuning

JPA Hibernate dirty-checking and session flushing consume memory and CPU, adding milliseconds to read-only endpoints.

### Read-Only Optimization
In `UserService.java`, we enabled class-level `@Transactional(readOnly = true)`.
* **Mechanism**: Instructs Hibernate to disable dirty-checking for entities retrieved during the transaction. No flush checks are executed, and Hibernate does not hold update locks on the retrieved entities.
* **Result**: Lower latency for user lookups, token validation, and login queries.

### Programmatic Normalized Tables
Instead of mapping all 64 child tables using complex JPA relationships (which would cause massive fetch chains, N+1 select queries, and high lazy-loading latencies), `TableDataPromotionService` syncs sections dynamically via programmatically constructed raw SQL inserts and deletes through `JdbcTemplate`. This guarantees maximum performance by bypassing ORM overhead entirely for bulk table updates.

---

## 4. HikariCP Connection Pool Latency

Connecting to **GCP Cloud SQL** on demand adds high TCP handshake latency.

### Socket Factory Integration
The application uses the `postgres-socket-factory`:
```xml
<dependency>
    <groupId>com.google.cloud.sql</groupId>
    <artifactId>postgres-socket-factory</artifactId>
</dependency>
```
This enables low-latency connection routing directly over Google's internal network using Unix sockets.

### Recommended Hikari Configuration (application.yaml)
To avoid thread-blocking latency when waiting for connections under traffic spikes, tweak connection pool settings:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 15     # Limit pool to prevent overwhelming Cloud SQL
      minimum-idle: 5           # Keep warm connections available
      connection-timeout: 20000 # 20 seconds maximum wait time
      idle-timeout: 300000      # 5 minutes idle time before release
      max-lifetime: 600000      # 10 minutes maximum lifetime
```

---

## 5. Recommended GCP Deployment Flags

To completely eliminate cold starts in production, apply these settings to your **Google Cloud Run** service:

### A. Keep Instances Warm (Min Instances)
Configure Cloud Run to maintain a minimum of 1 active container. This ensures that at least one warm instance is always available to handle traffic immediately:
```bash
gcloud run deploy director-appraisal-backend \
  --min-instances=1 \
  ...
```

### B. Allocate CPU Always
By default, Cloud Run throttle CPUs when requests are not actively processing, which can slow down background threads (like email mailer dispatching). Change allocation to "Always Allocated":
```bash
gcloud run deploy director-appraisal-backend \
  --no-cpu-throttling \
  ...
```

### C. Enable Startup Probes
Prevent Cloud Run from sending user requests to instances before they are fully booted. Add a startup probe pointing to `/actuator/health` (if actuator is enabled) or a lightweight ping endpoint:
```bash
gcloud run deploy director-appraisal-backend \
  --startup-cpu-boost \
  ...
```
*(Setting `--startup-cpu-boost` allocates double CPU capacity during startup, cutting container boot time in half.)*
