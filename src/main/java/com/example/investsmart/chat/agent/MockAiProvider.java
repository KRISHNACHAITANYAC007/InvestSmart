package com.example.investsmart.chat.agent;

/** Offline and deterministic; never produces financial claims or echoes sensitive questions. */
public final class MockAiProvider implements AiProvider {
    @Override public Answer respond(Request request) {
        return new Answer("[MOCK] AI foundation only. No portfolio was accessed or action performed. Educational simulation; not financial advice.", 0);
    }
}
