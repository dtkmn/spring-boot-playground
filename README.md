# Spring Boot Playground: A Comprehensive Template

## 1. Project Overview

Welcome to the Spring Boot Playground! This project serves as a modern, comprehensive template for building robust Spring Boot applications. It demonstrates a wide array of integrations and best practices, including Kafka messaging, R2DBC with PostgreSQL, Resilience4j for fault tolerance, Testcontainers for integration testing, Docker for containerization, and GitHub Actions for CI/CD.

The goal of this template is to provide a solid starting point for new projects, showcasing how these technologies can be effectively combined. It includes example REST controllers, Kafka producers/consumers/streams, reactive database interactions, and more.

## 2. Key Technologies

This template integrates the following key technologies:

*   **Framework**: Spring Boot 3.x
*   **Language**: Java 21
*   **Build Tool**: Gradle 8.x
*   **Messaging**: Apache Kafka, Spring Kafka, Kafka Streams
*   **Database**: PostgreSQL (Reactive with R2DBC)
*   **Resilience**: Resilience4j (Circuit Breaker)
*   **Web**: Spring WebFlux (Reactive Web)
*   **Testing**: JUnit 5, Mockito, Testcontainers (Kafka, PostgreSQL)
*   **Containerization**: Docker, Docker Compose
*   **CI/CD**: GitHub Actions
*   **Monitoring**: Spring Boot Actuator, Micrometer (Prometheus endpoint)
*   **Serialization**: (Implicitly JSON with Spring WebFlux/Web)

## 3. Prerequisites

Before you begin, ensure you have the following installed:

*   **JDK 21**: Java Development Kit, version 21.
*   **Gradle 8.x**: Or use the included Gradle Wrapper (`gradlew`).
*   **Docker**: For building and running Docker images and using Docker Compose.
*   **IDE**: Your preferred Java IDE (e.g., IntelliJ IDEA, Eclipse, VS Code).
*   **(Optional) Git**: For version control.

## 4. Building and Running the Application

### 4.1. Locally via IDE/Gradle

1.  **Clone the repository** (if you haven't already):
    ```bash
    git clone <repository-url>
    cd spring-boot-playground
    ```

2.  **Build the project using Gradle Wrapper**:
    ```bash
    ./gradlew clean build
    ```
    This command will compile the code, run tests, and package the application.

3.  **Run the application**:
    *   **Using Gradle**:
        ```bash
        ./gradlew bootRun
        ```
    *   **From your IDE**: Find the `SpringBootPlaygroundApplication` class and run its `main` method.

    The application will start using the default `application.yaml` configuration. For local development without Docker, you might need to have local instances of Kafka and PostgreSQL running, or adjust the configuration to use embedded alternatives if desired (though this template is primarily set up for Testcontainers or Dockerized services).

### 4.2. Using Docker Compose

This is the recommended way to run the application and its dependencies (Kafka, PostgreSQL, etc.) together.

1.  **Ensure Docker is running.**

2.  **Build the application JAR**:
    ```bash
    ./gradlew clean build -x test
    ```
    (Skipping tests here is optional but can speed up the process if tests have already passed). The `Dockerfile` will use this JAR.

3.  **Run with Docker Compose**:
    From the project root directory:
    ```bash
    docker-compose up --build
    ```
    This command will:
    *   Build the application's Docker image as defined in the `Dockerfile`.
    *   Start containers for the application, Kafka, Zookeeper, Schema Registry, Kafdrop, and PostgreSQL, as defined in `docker-compose.yml`.
    *   The application will use `application-docker.yaml` profile, which is configured to connect to the services within the Docker network.

    To stop the services:
    ```bash
    docker-compose down
    ```

## 5. Project Structure

The project follows a standard Maven/Gradle layout:

```
.
├── .github/workflows/        # GitHub Actions CI/CD workflows
├── build.gradle              # Gradle build script
├── data/                     # Sample data files
│   ├── crypto-symbols.txt
│   ├── inputs.txt
│   └── users.txt
├── docker-compose.yml        # Docker Compose configuration
├── Dockerfile                # Dockerfile for the application
├── gradle/                   # Gradle wrapper files
├── gradlew                   # Gradle wrapper executable (Linux/macOS)
├── gradlew.bat               # Gradle wrapper executable (Windows)
├── prometheus.yml            # Sample Prometheus configuration
├── scripts/                  # Utility scripts
│   └── bootstrap-topics.sh   # Kafka topic creation script
├── settings.gradle           # Gradle settings
└── src/
    ├── main/
    │   ├── java/playground/tech/springbootplayground/
    │   │   ├── SpringBootPlaygroundApplication.java  # Main application class
    │   │   ├── client/             # External API client examples (e.g., Binance)
    │   │   ├── config/             # Spring configuration classes
    │   │   ├── controller/         # REST API controllers
    │   │   ├── event/              # Kafka event definitions/POJOs
    │   │   ├── repository/         # R2DBC repositories
    │   │   ├── service/            # Business logic services
    │   │   └── streams/            # Kafka Streams topology
    │   └── resources/
    │       ├── application.yaml        # Default application configuration
    │       └── application-docker.yaml # Docker-specific configuration
    └── test/
        ├── java/playground/tech/springbootplayground/ # Unit and integration tests
        └── resources/
            └── application-test.yaml   # Configuration for tests (e.g., Testcontainers)
```

## 6. Configuration Files

*   **`src/main/resources/application.yaml`**:
    *   The default Spring Boot configuration file.
    *   Used when running the application locally without specific profiles activated (e.g., via `./gradlew bootRun` or IDE).
    *   Typically, this might be configured for local development against locally running services or embedded databases if not using Testcontainers for all tests.

*   **`src/main/resources/application-docker.yaml`**:
    *   A Spring profile-specific configuration file activated when the `docker` profile is active.
    *   Used by the application when running inside a Docker container managed by `docker-compose.yml`.
    *   Contains settings to connect to other services (Kafka, PostgreSQL) within the Docker network, using their service names as hostnames (e.g., `kafka:9092`).

*   **`src/test/resources/application-test.yaml`**:
    *   Configuration used when running tests.
    *   Often used to configure Testcontainers, embedded services, or mock external dependencies.

## 7. Docker Image Building and Usage

*   **`Dockerfile`**: Defines a multi-stage Docker build process:
    1.  **Builder Stage**: Copies Gradle files, downloads dependencies (leveraging Docker layer caching), copies source code, and builds the application JAR using Gradle. The JAR is renamed to `app.jar`.
    2.  **Final Stage**: Uses a minimal JRE base image (`eclipse-temurin:21-jre-ubi9-minimal`), creates a non-root user (`appuser`), copies the `app.jar` from the builder stage, and sets the `CMD` to run the application.

*   **Building the Image Manually**:
    ```bash
    docker build -t your-image-name:tag .
    ```

*   **Running with `docker-compose`**:
    As mentioned in section 4.2, `docker-compose.yml` handles building (if needed) and running the application image along with its dependencies. The service name in `docker-compose.yml` for the application is implicitly `app` if you were to add it, but this template primarily focuses on running the Spring Boot app directly via its Dockerfile build orchestrated by `docker-compose up --build`. The GitHub Actions workflow also builds and pushes an image to GHCR.

## 8. CI/CD (GitHub Actions)

The project includes a CI/CD pipeline defined in `.github/workflows/gradle.yml`:

*   **Trigger**: Runs on pushes to `main` and `dev` branches, and on pull requests targeting `main`.
*   **Jobs**:
    1.  **`build`**:
        *   Checks out the code.
        *   Sets up JDK 21.
        *   Caches Gradle dependencies.
        *   Sets up Gradle.
        *   Runs `./gradlew build --no-daemon` to compile, test, and build the application.
    2.  **`build-and-publish`**:
        *   Depends on the successful completion of the `build` job.
        *   Checks out code, sets up JDK and Gradle.
        *   Generates and submits a dependency graph to GitHub for Dependabot alerts.
        *   Logs in to GitHub Container Registry (GHCR).
        *   Builds the Docker image using the `Dockerfile`.
        *   Tags the image with the short commit SHA and `latest`.
        *   Pushes the image to GHCR. The `latest` tag is only pushed for builds on the `main` branch.

## 9. Sample Data and Kafka Bootstrap

*   **Sample Data Files (`data/`)**:
    *   `crypto-symbols.txt`: Sample cryptocurrency symbols for Kafka stream processing.
    *   `inputs.txt`: Generic input data, potentially for file-based Kafka producers.
    *   `users.txt`: Sample user data.
        These files are used by example components in the application, such as `SampleDataPublisherService`, to populate Kafka topics or demonstrate functionalities.

*   **Kafka Topic Bootstrapping (`scripts/bootstrap-topics.sh`)**:
    *   This script is executed by the `kafka` service in `docker-compose.yml`.
    *   It waits for Kafka to be available and then creates the necessary Kafka topics (`tweets`, `crypto-symbols`, `counts-tweets`, `formatted-tweets`, `alerts`, `reddit-posts`, `users`, `input-topic`) used by the application.
    *   This ensures that the application starts with all required topics present when running via Docker Compose.

## 10. Reference Documentation

For further reference, please consider the following sections from the Spring Boot documentation and other relevant projects:

*   [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/) (Use the version relevant to this project)
*   [Spring WebFlux](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#spring-webflux)
*   [Spring Data R2DBC](https://docs.spring.io/spring-data/relational/reference/r2dbc.html)
*   [Spring for Apache Kafka](https://docs.spring.io/spring-kafka/reference/index.html)
*   [Kafka Streams](https://kafka.apache.org/documentation/streams/)
*   [Project Reactor](https://projectreactor.io/docs/core/release/reference/)
*   [R2DBC (Reactive Relational Database Connectivity)](https://r2dbc.io/)
*   [Testcontainers](https://java.testcontainers.org/)
*   [Resilience4j](https://resilience4j.readme.io/)
*   [Docker Documentation](https://docs.docker.com/)
*   [GitHub Actions Documentation](https://docs.github.com/en/actions)
*   [Official Gradle documentation](https://docs.gradle.org)

*Note: Always refer to the specific versions of documentation relevant to the library versions used in this project (see `build.gradle` for details).*
