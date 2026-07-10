# =============================================================================
# Stage 1 — Build the Spring Boot fat jar (using Maven)
# =============================================================================
FROM maven:3.9.8-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src/main ./src/main
RUN mvn package -DskipTests -Dmaven.test.skip=true -B

# =============================================================================
# Stage 2 — Lean JRE runtime image
# =============================================================================
FROM eclipse-temurin:21-jre-alpine

# Run as a non-root user
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app


# Pre-create uploads dir for local fallback storage mode
# Pre-create uploads and logs dir for local fallback storage mode

RUN mkdir -p /app/uploads /app/logs && chown -R spring:spring /app/uploads /app/logs

COPY --from=build /app/target/*.jar app.jar

USER spring:spring

EXPOSE 8080

# Container-aware JVM: reads cgroup limits, caps heap at 75% of container RAM.
# Prevents OOM kills on Cloud Run's default instances.
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:InitialRAMPercentage=50.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.jmx.enabled=false", \
    "-Dfile.encoding=UTF-8", \
    "-jar", "app.jar"]
