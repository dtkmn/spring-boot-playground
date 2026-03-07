# Examples

Examples are outside the starter contract. They exist to show how to add optional integrations without forcing those choices onto every service.

Each example is self-contained:
- its own Gradle build
- its own Dockerfile and Docker Compose stack
- its own `.env.example`
- its own README

Current examples:
- `kafka-basic`: minimal HTTP producer and Kafka consumer flow
- `kafka-streams`: stream joins, enrichment, aggregation, and alerting
- `binance-websocket`: websocket ingestion into Kafka
