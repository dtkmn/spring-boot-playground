# Kafka Streams Example

This example demonstrates two common stream-processing patterns:
- topic joins and tweet enrichment
- trade aggregation and alert generation

## Run locally

```bash
./gradlew -p examples/kafka-streams test
cp examples/kafka-streams/.env.example examples/kafka-streams/.env
cd examples/kafka-streams && docker compose --env-file .env up --build
```

Bootstrap data is loaded into Kafka automatically when the example starts.

Spring Boot configures the Jackson 3 `JsonMapper` used by the trade parser and
Spring Kafka's `JacksonJsonSerde`. Kafka Streams uses Boot's managed version;
the former Kafka 3.9.2 override is incompatible with Spring Kafka 4.1's Streams
factory.

The test suite starts the Spring application context with broker connections
disabled, then runs two trades through the real topology with
`TopologyTestDriver` to check aggregation and JSON state/output serialization.
These tests run without Docker; the Compose path above exercises a real broker.

Default ports:
- app: `8083`
- Kafka broker: `9092`
- Kafdrop: `9000`
