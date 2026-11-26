package com.example.investsmart.chat.agent;

import com.example.investsmart.repository.UserRepository;
import com.example.investsmart.security.SecurityEvents;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/agent")
@Profile("(dev | test) & !prod")
@Conditional(LocalAgentCondition.class)
public class AgentPortfolioController {
    public enum Topic { OVERVIEW, UNREALIZED_PNL, CONCENTRATION }
    public record Request(Topic topic, boolean consent) {
        static Request parse(JsonNode body) {
            if(body==null||!body.isObject()||body.size()!=2||!body.path("topic").isTextual()
                    ||!body.path("consent").isBoolean()||!body.path("consent").asBoolean())throw invalid();
            try{return new Request(Topic.valueOf(body.path("topic").asText()),true);}
            catch(IllegalArgumentException badTopic){throw invalid();}
        }
        private static ResponseStatusException invalid(){return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a supported portfolio question and explicitly consent.");}
    }
    public record Reply(Topic topic, String mode, PortfolioAnalysisService.Analysis analysis) {}
    private final PortfolioAnalysisService analysis;
    private final UserRepository users;
    private final SecurityEvents events;
    public AgentPortfolioController(PortfolioAnalysisService analysis,UserRepository users,SecurityEvents events){
        this.analysis=analysis;this.users=users;this.events=events;
    }
    @PostMapping("/portfolio-analysis")
    public Reply analyze(Principal principal,@RequestBody JsonNode body,HttpServletRequest request){
        var input=Request.parse(body);
        if(principal==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var user=users.findByEmailIgnoreCase(principal.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        user.requireActive();
        events.limit("AGENT_PORTFOLIO",user.getEmail(),request,10,30,user.getId());
        return new Reply(input.topic(),"LOCAL_DETERMINISTIC",analysis.analyze(principal,input.consent()));
    }
}
