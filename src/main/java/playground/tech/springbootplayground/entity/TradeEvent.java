package playground.tech.springbootplayground.entity;

public record TradeEvent(String symbol, double price, double quantity, long timestamp) {}