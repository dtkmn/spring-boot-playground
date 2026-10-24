FROM gradle:9.8.2-jdk21 AS builder
WORKDIR /usr/src/app
COPY src ./src
COPY build.gradle .
COPY settings.gradle .
RUN gradle --no-daemon bootJar -x test

FROM gcr.io/distroless/java21-debian13:nonroot@sha256:f3ac871fb5802c2739bb23b67212e67f341cbe754131528d2e9849072f9efb9a
WORKDIR /app
COPY --from=builder /usr/src/app/build/libs/app.jar ./app.jar
EXPOSE 8080
USER 65532:65532
CMD ["app.jar"]
