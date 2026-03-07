# ADR-001: Default Starter And Variant Strategy

## Status
Accepted

## Context
The repository started as a broad Spring playground. That shape is poor for a golden-path starter because engineers inherit unnecessary dependencies and unclear runtime choices.

## Decision
- `mvc-jpa` is the default starter.
- `webflux-r2dbc` is a supported advanced variant.
- Optional integrations belong in `examples/`, not in the starter core.

## Consequences
- Most new services should start from `mvc-jpa`.
- Reactive support remains available without forcing that complexity onto all teams.
- Kafka, Kafka Streams, websocket ingest, and similar assets are treated as examples rather than starter requirements.
