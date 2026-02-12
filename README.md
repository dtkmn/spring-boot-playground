![GitHub stars](https://img.shields.io/github/stars/dtkmn/spring-boot-playground?style=social)
![GitHub forks](https://img.shields.io/github/forks/dtkmn/spring-boot-playground?style=social)
![GitHub watchers](https://img.shields.io/github/watchers/dtkmn/spring-boot-playground?style=social)
![GitHub repo size](https://img.shields.io/github/repo-size/dtkmn/spring-boot-playground)
![GitHub language count](https://img.shields.io/github/languages/count/dtkmn/spring-boot-playground)
![GitHub top language](https://img.shields.io/github/languages/top/dtkmn/spring-boot-playground)
![GitHub last commit](https://img.shields.io/github/last-commit/dtkmn/spring-boot-playground?color=red)
![GitHub Tag](https://img.shields.io/github/v/tag/dtkmn/spring-boot-playground)

# Spring Boot Playground: A Comprehensive Template

## 1. Project Overview

Welcome to the Spring Boot Playground! This project serves as a modern, comprehensive template for building robust Spring Boot applications. It demonstrates a wide array of integrations and best practices, including Kafka messaging, R2DBC with PostgreSQL, Resilience4j for fault tolerance, Testcontainers for integration testing, Docker for containerization, and GitHub Actions for CI/CD.

The goal of this template is to provide a solid starting point for new projects, showcasing how these technologies can be effectively combined. It includes example REST controllers, Kafka producers/consumers/streams, reactive database interactions, and more.

## 2. Key Technologies

This template integrates the following key technologies:

*   **Framework**: Spring Boot 3.x
*   **Language**: Java 21
*   **Build Tool**: Gradle 9.x
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
*   **Gradle 9.x**: Or use the included Gradle Wrapper (`gradlew`).
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

2.  **Provide required environment variables**:
    ```bash
    cp .env.example .env
    ```
    At minimum, set `POSTGRES_PASSWORD` in `.env`.

3.  **(Optional) Build locally first**:
    ```bash
    ./gradlew clean build -x test
    ```
    This is optional because the `Dockerfile` builds the application inside the container build stage.

4.  **Run with Docker Compose**:
    From the project root directory:
    ```bash
    docker compose up --build
    ```
    This command will:
    *   Build the application's Docker image as defined in the `Dockerfile`.
    *   Start containers for the application, Kafka (KRaft mode), PostgreSQL, and Kafdrop, as defined in `docker-compose.yml`.
    *   (Optional) Schema Registry remains as commented example configuration.
    *   The application will use `application-docker.yaml` profile, which is configured to connect to the services within the Docker network.

    To stop the services:
    ```bash
    docker compose down
    ```

## 5. Project Structure

The project follows a standard Maven/Gradle layout:

```
.
├── .github/workflows/        # GitHub Actions CI/CD workflows
├── .env.example              # Sample environment variables for local/docker runs
├── .gitignore                # Repository ignore rules
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
    │   │   ├── configuration/      # Spring configuration classes
    │   │   ├── controller/         # REST API controllers
    │   │   ├── entity/             # Domain entities/DTOs
    │   │   ├── repository/         # R2DBC repositories
    │   │   ├── service/            # Business logic services
    │   │   └── streams/            # Kafka Streams topology
    │   └── resources/
    │       ├── application.yaml        # Default application configuration
    │       └── application-docker.yaml # Docker-specific configuration
    └── test/
        └── java/playground/tech/springbootplayground/ # Unit and integration tests
```

## 6. Configuration Files

*   **`src/main/resources/application.yaml`**:
    *   The default Spring Boot configuration file.
    *   Used when running the application locally without specific profiles activated (e.g., via `./gradlew bootRun` or IDE).
    *   Database connection values are sourced from `POSTGRES_*` environment variables.
    *   Kafka Streams console debug printing is disabled by default (`app.streams.debug-print=false`).

*   **`src/main/resources/application-docker.yaml`**:
    *   A Spring profile-specific configuration file activated when the `docker` profile is active.
    *   Used by the application when running inside a Docker container managed by `docker-compose.yml`.
    *   Contains settings to connect to Kafka/PostgreSQL in the Docker network, and expects `POSTGRES_PASSWORD` to be provided by environment.

*   **Tests configuration**:
    *   Integration tests use Testcontainers and `@DynamicPropertySource` to inject runtime Kafka endpoints.

## 7. Docker Image Building and Usage

*   **`Dockerfile`**: Defines a multi-stage Docker build process:
    1.  **Builder Stage**: Copies source and Gradle files, then builds the Spring Boot executable jar (`bootJar`).
    2.  **Final Stage**: Uses a minimal JRE base image (`eclipse-temurin:21-jre-ubi9-minimal`), copies `app.jar` from the builder stage, and starts it with `java -jar`.

*   **Building the Image Manually**:
    ```bash
    docker build -t your-image-name:tag .
    ```

*   **Running with Docker Compose**:
    As mentioned in section 4.2, `docker-compose.yml` handles building and running the application image with its dependencies (`docker compose up --build`).

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
        *   Runs only for direct pushes to the `main` branch.
        *   Checks out code, sets up JDK and Gradle.
        *   Generates and submits a dependency graph to GitHub for Dependabot alerts.
        *   Logs in to GitHub Container Registry (GHCR).
        *   Builds the Docker image using the `Dockerfile`.
        *   Tags the image with the commit SHA.
        *   Pushes the image to GHCR.

## 9. Sample Data and Kafka Bootstrap

*   **Sample Data Files (`data/`)**:
    *   `crypto-symbols.txt`: Sample cryptocurrency symbols for Kafka stream processing.
    *   `inputs.txt`: Generic input data, potentially for file-based Kafka producers.
    *   `users.txt`: Sample user data.
        These files are used by `scripts/bootstrap-topics.sh` to pre-populate Kafka topics for local runs.

*   **Kafka Topic Bootstrapping (`scripts/bootstrap-topics.sh`)**:
    *   This script is executed by the `kafka` service in `docker-compose.yml`.
    *   It waits for Kafka to be available and then creates required topics (`tweets`, `formatted-tweets`, `users`, `crypto-symbols`).
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
