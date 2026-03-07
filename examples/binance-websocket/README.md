# Binance Websocket Example

This example shows a websocket ingestion pattern that reads Binance spot trades and forwards them into Kafka.

## What it demonstrates
- Spring WebFlux websocket client
- resilience on long-lived websocket connections
- Kafka producer integration for downstream processing

## Run locally

```bash
./gradlew -p examples/binance-websocket test
cp examples/binance-websocket/.env.example examples/binance-websocket/.env
cd examples/binance-websocket && docker compose --env-file .env up --build
```

The app publishes trade messages to the `crypto-prices` topic.

Default ports:
- app: `8082`
- Kafka broker: `9092`
- Kafdrop: `9000`

Set `BINANCE_WS_URL` in `.env` if you need to point the example at a non-default websocket endpoint.
