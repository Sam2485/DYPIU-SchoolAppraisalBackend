# =============================================================================
# Stage 1 — Build the Spring Boot fat jar (using Gradle)
# =============================================================================
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY gradlew .
COPY gradle/ gradle/
RUN chmod +x gradlew

COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon

COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# =============================================================================
# Stage 2 — Lean JRE runtime image
# =============================================================================
FROM eclipse-temurin:21-jre-alpine

# Run as a non-root user
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Pre-create uploads dir for local fallback storage mode
RUN mkdir -p /app/uploads && chown spring:spring /app/uploads

COPY --from=build /app/build/libs/*.jar app.jar

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
