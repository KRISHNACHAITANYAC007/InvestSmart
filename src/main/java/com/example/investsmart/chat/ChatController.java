package com.example.investsmart.chat;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="investsmart.features.learning-enabled",havingValue="true")
@RequestMapping("/learning/chat")
public class ChatController {
    public record Request(
            @NotBlank @Size(min = 3, max = 1000) String question,
            @NotNull @AssertTrue Boolean providerConsent,
            boolean includePortfolio) {}

    private final LearningAssistant assistant;

    public ChatController(LearningAssistant assistant) {
        this.assistant = assistant;
    }

    @PostMapping
    public LearningAssistant.Answer ask(Principal principal, HttpServletRequest request,
                                        @Valid @RequestBody Request body) {
        return assistant.answer(principal.getName(), body.question(), body.includePortfolio(), request);
    }
}
