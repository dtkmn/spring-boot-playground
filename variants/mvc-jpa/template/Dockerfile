FROM gradle:9.6.1-jdk21 AS builder
WORKDIR /usr/src/app
COPY src ./src
COPY build.gradle .
COPY settings.gradle .
RUN gradle --no-daemon bootJar -x test

FROM gcr.io/distroless/java21-debian13:nonroot@sha256:8ee299a87382317dd56ee4ffdd9c48f0541f1afe33ea2b64f2f880decd82acf6
WORKDIR /app
COPY --from=builder /usr/src/app/build/libs/app.jar ./app.jar
EXPOSE __APP_PORT__ __MANAGEMENT_PORT__
USER 65532:65532
CMD ["app.jar"]
