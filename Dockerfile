# Use the official gradle image to create a build artifact.
# This is based on Debian and sets the GRADLE_HOME environment variable
FROM gradle:8.14.2-jdk21 as builder

# Set the working directory in the image
WORKDIR /usr/src/app

# Copy your source code to the image
COPY src ./src
COPY build.gradle .
COPY settings.gradle .

# Package the application
RUN gradle build -x test

# Use eclipse-temurin as the base image
FROM eclipse-temurin:21-jre-ubi9-minimal

WORKDIR /app

# Copy the jar file from the build stage
COPY --from=builder /usr/src/app/build/libs/spring-boot-playground-0.0.*.jar ./spring-boot-playground.jar

# Expose the port your app runs on
EXPOSE 8080 9010

# Start your application
CMD ["java", "-jar", "./spring-boot-playground.jar"]
