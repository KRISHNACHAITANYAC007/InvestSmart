package com.example.investsmart.chat.agent;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** No expression interpolation: only explicit flag plus mock mode permits the local interface. */
public class LocalAgentCondition implements Condition {
    @Override public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        var env=context.getEnvironment();
        return "true".equals(env.getProperty("investsmart.features.agent-enabled"))
                && "mock".equals(env.getProperty("investsmart.ai.agent.provider"));
    }
}
