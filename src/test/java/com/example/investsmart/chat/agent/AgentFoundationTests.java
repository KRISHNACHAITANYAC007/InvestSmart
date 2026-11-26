package com.example.investsmart.chat.agent;

import com.example.investsmart.entity.User;
import com.example.investsmart.repository.UserRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AgentFoundationTests {
    final UserRepository users=mock(UserRepository.class);
    AgentFoundation service(AiProvider provider) {
        when(users.findByEmailIgnoreCase("owner@example.invalid")).thenReturn(Optional.of(new User()));
        return new AgentFoundation(users,provider,512);
    }
    @Test void mockIsOfflineDoesNotEchoQuestionOrAccessPortfolio() {
        var r=service(new MockAiProvider()).respond(()->"owner@example.invalid","private synthetic text",true);
        assertTrue(r.text().startsWith("[MOCK]"));assertFalse(r.text().contains("private synthetic"));
        verify(users).findByEmailIgnoreCase("owner@example.invalid");verifyNoMoreInteractions(users);
    }
    @Test void modelNeverReceivesServerIdentity() {
        service(request->{assertEquals("educational question",request.question());assertEquals(512,request.maxOutputTokens());return new AiProvider.Answer("answer",1);})
                .respond(()->"owner@example.invalid","educational question",true);
    }
    @Test void absentUnknownOrSuspendedIdentityCannotCallProvider() {
        var provider=mock(AiProvider.class);var f=service(provider);
        assertThrows(ResponseStatusException.class,()->f.respond(null,"question",true));
        when(users.findByEmailIgnoreCase("foreign")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class,()->f.respond(()->"foreign","question",true));
        var u=new User();u.setAccountStatus("SUSPENDED");when(users.findByEmailIgnoreCase("owner@example.invalid")).thenReturn(Optional.of(u));
        assertThrows(ResponseStatusException.class,()->f.respond(()->"owner@example.invalid","question",true));verifyNoInteractions(provider);
    }
    @Test void consentQuestionAndBudgetAreBounded() {
        var provider=mock(AiProvider.class);var f=service(provider);
        for(String q:new String[]{null," ","x".repeat(1001)})assertThrows(ResponseStatusException.class,()->f.respond(()->"owner@example.invalid",q,true));
        assertThrows(ResponseStatusException.class,()->f.respond(()->"owner@example.invalid","question",false));
        assertThrows(IllegalArgumentException.class,()->new AgentFoundation(users,provider,2049));verifyNoInteractions(provider);
    }
    @Test void providerFailureAndInvalidOutputAreSanitized() {
        for(AiProvider provider:List.<AiProvider>of(r->{throw new RuntimeException("synthetic-secret");},r->new AiProvider.Answer("x",513),r->null,r->new AiProvider.Answer("x".repeat(8001),1))) {
            var e=assertThrows(ResponseStatusException.class,()->service(provider).respond(()->"owner@example.invalid","question",true));
            assertEquals(503,e.getStatusCode().value());assertFalse(e.getMessage().contains("synthetic-secret"));
        }
    }
    @Test void toolRegistryDeniesIdentityUnknownToolsAndUnexpectedArguments() {
        assertThrows(IllegalArgumentException.class,()->new ToolRegistry.Definition("getPortfolio",Set.of("userId")));
        assertThrows(IllegalArgumentException.class,()->new ToolRegistry.Definition("getPortfolio",Set.of("USERID")));
        var r=new ToolRegistry(List.of(new ToolRegistry.Definition("explain",Set.of("topic"))));
        assertThrows(IllegalArgumentException.class,()->r.validate("executeOrder",Map.of()));
        assertThrows(IllegalArgumentException.class,()->r.validate("explain",Map.of("topic","risk","userId","2")));
        assertEquals("explain",r.validate("explain",Map.of("topic","risk")).name());
        assertThrows(IllegalArgumentException.class,()->new ToolRegistry(List.of(new ToolRegistry.Definition("same",Set.of()),new ToolRegistry.Definition("same",Set.of()))));
    }
    ApplicationContextRunner context(String... profiles) {
        return new ApplicationContextRunner().withInitializer(c->c.getEnvironment().setActiveProfiles(profiles))
                .withBean(com.example.investsmart.service.PortfolioService.class,()->mock(com.example.investsmart.service.PortfolioService.class))
                .withBean(com.example.investsmart.repository.StockRepository.class,()->mock(com.example.investsmart.repository.StockRepository.class))
                .withUserConfiguration(AgentConfiguration.class).withBean(UserRepository.class,()->users);
    }
    @Test void defaultOffAndProdNeverRegisterMockOrFoundation() {
        context("test").run(c->assertFalse(c.containsBean("agentFoundation")));
        context("prod","test").withPropertyValues("investsmart.features.agent-enabled=true","investsmart.ai.agent.provider=mock")
                .run(c->{assertFalse(c.containsBean("agentFoundation"));assertFalse(c.containsBean("mockAgentProvider"));});
    }
    @Test void controllerIsAbsentWithMissingFlagUnknownProviderOrProductionProfile() {
        context("test").withUserConfiguration(AgentPortfolioController.class).withPropertyValues("investsmart.ai.agent.provider=mock")
                .run(c->assertFalse(c.containsBean("agentPortfolioController")));
        context("test").withUserConfiguration(AgentPortfolioController.class).withPropertyValues("investsmart.features.agent-enabled=true","investsmart.ai.agent.provider=unknown")
                .run(c->assertFalse(c.containsBean("agentPortfolioController")));
        context("prod","test").withUserConfiguration(AgentPortfolioController.class).withPropertyValues("investsmart.features.agent-enabled=true","investsmart.ai.agent.provider=mock")
                .run(c->assertFalse(c.containsBean("agentPortfolioController")));
    }
    @Test void explicitLocalMockRegistersOnlyReadToolAndAnalysisWithoutSecretsOrNetwork() {
        context("test").withPropertyValues("investsmart.features.agent-enabled=true","investsmart.ai.agent.provider=mock")
                .run(c->{assertNull(c.getStartupFailure());assertEquals(1,c.getBean(ToolRegistry.class).size());assertNotNull(c.getBean(AgentFoundation.class));assertNotNull(c.getBean(PortfolioReadTool.class));assertNotNull(c.getBean(PortfolioAnalysisService.class));});
        context("test").withPropertyValues("investsmart.features.agent-enabled=true","investsmart.ai.agent.provider=unknown")
                .run(c->assertFalse(c.containsBean("agentFoundation")));
    }
}
