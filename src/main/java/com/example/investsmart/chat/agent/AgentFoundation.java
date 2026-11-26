package com.example.investsmart.chat.agent;

import com.example.investsmart.repository.UserRepository;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Internal consent-aware single-turn boundary, not the AI-4 agent loop or an HTTP endpoint. */
public final class AgentFoundation {
    private final UserRepository users;
    private final AiProvider provider;
    private final int outputTokenLimit;
    public AgentFoundation(UserRepository users, AiProvider provider, int outputTokenLimit) {
        if (outputTokenLimit < 1 || outputTokenLimit > 2048) throw new IllegalArgumentException("Invalid output budget");
        this.users = users; this.provider = provider; this.outputTokenLimit = outputTokenLimit;
    }
    public AiProvider.Answer respond(Principal principal, String question, boolean consent) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var user = users.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        user.requireActive();
        if (!consent || question == null || question.isBlank() || question.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consent and a bounded question are required.");
        }
        try {
            var result = provider.respond(new AiProvider.Request(question, outputTokenLimit));
            if (result == null || result.text() == null || result.text().isBlank() || result.text().length() > 8000
                    || result.outputTokens() < 0 || result.outputTokens() > outputTokenLimit) throw new IllegalStateException();
            return result;
        } catch (RuntimeException unavailable) {
            // Do not return/log provider exception bodies, secrets or user text.
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI is temporarily unavailable.");
        }
    }
}
