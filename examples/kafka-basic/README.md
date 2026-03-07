# Kafka Basic Example

This example demonstrates a minimal HTTP-to-Kafka flow.

## Run locally

```bash
./gradlew -p examples/kafka-basic test
cp examples/kafka-basic/.env.example examples/kafka-basic/.env
cd examples/kafka-basic && docker compose --env-file .env up --build
curl -X POST http://localhost:8084/api/v1/messages -H 'Content-Type: application/json' -d '{"key":"demo","payload":"hello world"}'
```

Default ports:
- app: `8084`
- Kafka broker: `9092`
- Kafdrop: `9000`
