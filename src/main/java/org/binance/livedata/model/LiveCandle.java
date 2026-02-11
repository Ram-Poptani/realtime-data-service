package org.binance.livedata.model;

import lombok.Data;

@Data
public class LiveCandle {

    private String symbol;
    private long openTime;
    private long closeTime;
    private double open;
    private double high;
    private double low;
    private double close;
    private double volume;
    private int tradeCount;

    public synchronized void addTrade(double price, double quantity, long tradeTime) {
        if (tradeCount == 0) {
            this.open = price;
            this.high = price;
            this.low = price;
            this.openTime = tradeTime;
        } else {
            this.high = Math.max(this.high, price);
            this.low = Math.min(this.low, price);
        }
        this.close = price;
        this.closeTime = tradeTime;
        this.volume += quantity;
        this.tradeCount++;
    }

    public synchronized LiveCandle snapshot() {
        LiveCandle copy = new LiveCandle();
        copy.symbol = this.symbol;
        copy.openTime = this.openTime;
        copy.closeTime = this.closeTime;
        copy.open = this.open;
        copy.high = this.high;
        copy.low = this.low;
        copy.close = this.close;
        copy.volume = this.volume;
        copy.tradeCount = this.tradeCount;
        return copy;
    }

    public synchronized void reset() {
        this.open = 0;
        this.high = 0;
        this.low = 0;
        this.close = 0;
        this.volume = 0;
        this.tradeCount = 0;
        this.openTime = 0;
        this.closeTime = 0;
    }
}