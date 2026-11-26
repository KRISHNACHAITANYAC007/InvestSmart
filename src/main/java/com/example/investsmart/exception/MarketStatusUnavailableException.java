package com.example.investsmart.exception;

public class MarketStatusUnavailableException extends RuntimeException {
    private final int retryAfterSeconds;
    public MarketStatusUnavailableException(String message, Throwable cause) { this(message,cause,15); }
    public MarketStatusUnavailableException(String message, Throwable cause, int retryAfterSeconds) {
        super(message,cause); this.retryAfterSeconds=Math.max(1,Math.min(86400,retryAfterSeconds));
    }
    public int getRetryAfterSeconds(){return retryAfterSeconds;}
}
