package com.example.investsmart.chat.agent;

import com.example.investsmart.repository.UserRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

/** Explicit local mock opt-in only; false/missing flag or prod profile creates no agent beans. */
@Configuration(proxyBeanMethods = false)
@Profile("(dev | test) & !prod")
@ConditionalOnProperty(name = "investsmart.features.agent-enabled", havingValue = "true")
public class AgentConfiguration {
        @ConditionalOnProperty(name = "investsmart.ai.agent.provider", havingValue = "mock")
        @Bean AiProvider mockAgentProvider() { return new MockAiProvider(); }
        @ConditionalOnProperty(name = "investsmart.ai.agent.provider", havingValue = "mock")
        @Bean ToolRegistry agentToolRegistry() { return new ToolRegistry(List.of(new ToolRegistry.Definition("getMyPortfolio",java.util.Set.of()))); }
        @ConditionalOnProperty(name = "investsmart.ai.agent.provider", havingValue = "mock")
        @Bean PortfolioReadTool portfolioReadTool(UserRepository users,
                com.example.investsmart.service.PortfolioService portfolios,
                com.example.investsmart.repository.StockRepository stocks, ToolRegistry registry) {
            return new PortfolioReadTool(users,portfolios,stocks,registry,java.time.Clock.systemUTC());
        }
        @ConditionalOnProperty(name = "investsmart.ai.agent.provider", havingValue = "mock")
        @Bean PortfolioAnalysisService portfolioAnalysisService(PortfolioReadTool portfolio) {
            return new PortfolioAnalysisService(portfolio);
        }
        @ConditionalOnProperty(name = "investsmart.ai.agent.provider", havingValue = "mock")
        @Bean AgentFoundation agentFoundation(UserRepository users, AiProvider provider,
                @Value("${investsmart.ai.agent.max-output-tokens:512}") int tokens) {
            return new AgentFoundation(users, provider, tokens);
        }
}
