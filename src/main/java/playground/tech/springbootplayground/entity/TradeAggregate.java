package playground.tech.springbootplayground.entity;

public class TradeAggregate {
    private double totalPrice;
    private long tradeCount;

    public TradeAggregate() {
        this.totalPrice = 0.0;
        this.tradeCount = 0;
    }

    public TradeAggregate add(TradeEvent trade) {
        this.totalPrice += trade.price();
        this.tradeCount++;
        return this;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public long getTradeCount() {
        return tradeCount;
    }
}
