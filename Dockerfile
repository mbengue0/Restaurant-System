# syntax=docker/dockerfile:1
# Multi-stage build: compile the jar in a JDK image, run it in a JRE image.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy the Maven wrapper + descriptor first so dependency resolution
# can be cached when only source changes.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Now copy the source and build.
COPY src ./src
RUN ./mvnw -B -q clean package -DskipTests \
    && mv target/restaurant-system-*.jar /tmp/app.jar

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as an unprivileged user.
RUN groupadd --system app && useradd --system --gid app --home /app app
COPY --from=build --chown=app:app /tmp/app.jar /app/app.jar
USER app

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
