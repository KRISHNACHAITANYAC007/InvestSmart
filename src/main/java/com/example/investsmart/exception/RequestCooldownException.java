package com.example.investsmart.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RequestCooldownException extends ResponseStatusException {
    private final int seconds;
    public RequestCooldownException(String action,int seconds){
        super(HttpStatus.TOO_MANY_REQUESTS,"Too many "+action+" requests. Try again in "+seconds+" seconds. Refreshing or restarting does not clear this limit.");
        this.seconds=seconds;
    }
    @Override public HttpHeaders getHeaders(){var headers=new HttpHeaders();headers.set(HttpHeaders.RETRY_AFTER,Integer.toString(seconds));return headers;}
}
