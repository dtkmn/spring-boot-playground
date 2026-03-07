package tech.dtkmn.examples.kafkastreams.entity;

public record TradeEvent(String symbol, double price, double quantity, long timestamp) {}