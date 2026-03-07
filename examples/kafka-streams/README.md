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

Default ports:
- app: `8083`
- Kafka broker: `9092`
- Kafdrop: `9000`
