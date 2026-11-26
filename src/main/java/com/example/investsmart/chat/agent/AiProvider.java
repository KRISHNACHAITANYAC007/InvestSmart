package com.example.investsmart.chat.agent;

/** No identity or credentials in model-visible requests. A live adapter needs a separate review. */
public interface AiProvider {
    record Request(String question, int maxOutputTokens) {}
    record Answer(String text, int outputTokens) {}
    Answer respond(Request request);
}
