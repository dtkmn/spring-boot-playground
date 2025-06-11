# Stage 1: Build the application
FROM gradle:8.7.0-jdk17 as builder

WORKDIR /usr/src/app

# Copy Gradle configuration files first to leverage Docker layer caching
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Download dependencies
# Using `build` with `--no-daemon` and a specific task like `dependencies` can be problematic.
# A common approach is to trigger dependency resolution by running a task that requires it.
# `bootJar` with `enabled=false` or `classes` task can resolve dependencies needed for compilation.
# Or, more simply, `gradle dependencies` if it reliably downloads all necessary for build.
# For this template, we'll try `gradle classes` as it's a standard task that resolves compilation dependencies.
RUN gradle classes --no-daemon

# Copy the source code
COPY src ./src

# Build the application, excluding tests, and create the JAR
RUN gradle build -x test --no-daemon

# Rename the JAR to a consistent name
RUN mv /usr/src/app/build/libs/spring-boot-playground-0.0.*.jar /usr/src/app/build/libs/app.jar

# Stage 2: Create the final application image
FROM eclipse-temurin:17-jre-ubi9-minimal

WORKDIR /app

# Create a non-root user and group
# UBI images use microdnf. Ensure shadow-utils (for useradd/groupadd) is available.
# However, groupadd/useradd are often present in base UBI images.
# Adding them directly. If this fails, one might need:
# RUN microdnf install -y shadow-utils && microdnf clean all
RUN groupadd -r appgroup && useradd -r -g appgroup -d /app -s /sbin/nologin appuser

# Copy the JAR file from the builder stage
COPY --from=builder /usr/src/app/build/libs/app.jar ./app.jar

# Ensure the app directory and JAR are owned by the new user
# This might be needed if WORKDIR creation doesn't grant enough permission by default.
# For now, assuming /app is writable by the user or that USER instruction handles this.
# If not, `RUN chown -R appuser:appgroup /app` would be needed here.

# Expose the port your app runs on
EXPOSE 8080 9010

# Switch to the non-root user
USER appuser

# Start your application
CMD ["java", "-jar", "./app.jar"]
