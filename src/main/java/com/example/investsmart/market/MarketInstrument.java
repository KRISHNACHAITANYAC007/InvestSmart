package com.example.investsmart.market;

public class MarketInstrument {

    private final String instrumentKey;
    private final String symbol;
    private final String companyName;
    private final String exchange;
    private final String isin;

    public MarketInstrument(
            String instrumentKey,
            String symbol,
            String companyName,
            String exchange) {
        this(instrumentKey, symbol, companyName, exchange,
                instrumentKey != null && instrumentKey.startsWith("NSE_EQ|")
                        ? instrumentKey.substring(7) : null);
    }

    public MarketInstrument(String instrumentKey, String symbol, String companyName, String exchange, String isin) {
        this.instrumentKey = instrumentKey;
        this.symbol = symbol;
        this.companyName = companyName;
        this.exchange = exchange;
        this.isin = isin;
    }

    public String getInstrumentKey() {
        return instrumentKey;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getExchange() {
        return exchange;
    }
    public String getIsin() { return isin; }
}
