# syntax=docker/dockerfile:1

# --- build -------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Dependencies resolve in their own layer so a source-only change does not
# re-download the world on every build.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Tests already ran in CI before this image is built; running them again here
# would need a Docker daemon inside the build for Testcontainers.
RUN mvn -B -q clean package -DskipTests

# --- run ---------------------------------------------------------------------
# jammy, not alpine: eclipse-temurin:17-jre-alpine publishes amd64 only, so an
# alpine base cannot be built or run on an arm64 machine. jammy is multi-arch.
FROM eclipse-temurin:17-jre-jammy AS runtime

# Non-root. The app needs no write access to its own filesystem.
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app

COPY --from=build /build/target/*.jar app.jar
USER app

EXPOSE 8080

# The prod profile is baked in: this image only exists to be deployed, and
# leaving profile selection to the caller invites a pod that silently boots with
# dev defaults. Everything genuinely environment-specific arrives as env vars.
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
