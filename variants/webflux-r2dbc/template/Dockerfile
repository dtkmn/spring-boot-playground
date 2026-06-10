FROM gradle:9.5.1-jdk21 AS builder
WORKDIR /usr/src/app
COPY src ./src
COPY build.gradle .
COPY settings.gradle .
RUN gradle --no-daemon bootJar -x test

FROM eclipse-temurin:25-jre-ubi10-minimal
WORKDIR /app
COPY --from=builder /usr/src/app/build/libs/app.jar ./app.jar
EXPOSE __APP_PORT__ __MANAGEMENT_PORT__
USER 10001:10001
CMD ["java", "-jar", "./app.jar"]
