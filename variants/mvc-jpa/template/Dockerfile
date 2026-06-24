FROM gradle:9.6.0-jdk21 AS builder
WORKDIR /usr/src/app
COPY src ./src
COPY build.gradle .
COPY settings.gradle .
RUN gradle --no-daemon bootJar -x test

FROM eclipse-temurin:21.0.11_10-jre-alpine-3.22
WORKDIR /app
COPY --from=builder /usr/src/app/build/libs/app.jar ./app.jar
EXPOSE __APP_PORT__ __MANAGEMENT_PORT__
USER 10001:10001
CMD ["java", "-jar", "./app.jar"]
