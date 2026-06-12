# syntax=docker/dockerfile:1
# ---- Build stage: compile + package with the committed Maven wrapper (reproducible, no system Maven) ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Warm the dependency cache first so code-only changes don't re-download the world.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Tests run in CI against Testcontainers (needs a Docker daemon); skip them in the image build.
RUN ./mvnw -B -q clean package -DskipTests

# ---- Runtime stage: slim JRE, non-root ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
RUN groupadd --system cafe && useradd --system --gid cafe cafe
COPY --from=build /app/target/*.jar app.jar
USER cafe
EXPOSE 8080
# Honors container memory limits; override JAVA_OPTS at deploy time if needed.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
