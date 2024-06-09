# Use the official gradle image to create a build artifact.
# This is based on Debian and sets the GRADLE_HOME environment variable
FROM gradle:8.7.0-jdk17 as builder

# Set the working directory in the image
WORKDIR /usr/src/app

# Copy your source code to the image
COPY src ./src
COPY build.gradle .
COPY settings.gradle .

# Package the application
RUN gradle build -x test

# Use eclipse-temurin:20-jre-alpine for the runtime stage of the image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the jar file from the build stage
COPY --from=builder /usr/src/app/build/libs/spring-boot-playground-0.0.*.jar ./spring-boot-playground.jar

# Expose the port your app runs on
EXPOSE 8080

# Start your application
CMD ["java", "-jar", "./spring-boot-playground.jar"]
