FROM gradle:9.6.1-jdk21 AS builder
WORKDIR /usr/src/app
COPY src ./src
COPY build.gradle .
COPY settings.gradle .
RUN gradle --no-daemon bootJar -x test

FROM gcr.io/distroless/java21-debian13:nonroot@sha256:e7c40c2378f8c4462aea63041f6b29b7f2b9ab8e80b9217d6d5faf70dfff1779
WORKDIR /app
COPY --from=builder /usr/src/app/build/libs/app.jar ./app.jar
EXPOSE 8080
USER 65532:65532
CMD ["app.jar"]
