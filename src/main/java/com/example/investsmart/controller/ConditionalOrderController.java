package com.example.investsmart.controller;
import com.example.investsmart.service.ConditionalOrders;
import com.example.investsmart.security.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/orders/conditional")
public class ConditionalOrderController {
    private final ConditionalOrders service;private final SecurityAccounts accounts;private final SecurityEvents events;
    public ConditionalOrderController(ConditionalOrders service,SecurityAccounts accounts,SecurityEvents events){this.service=service;this.accounts=accounts;this.events=events;}
    @GetMapping public List<Map<String,Object>> list(Authentication auth){return service.list(auth.getName());}
    @PostMapping public Map<String,Object> submit(Authentication auth,@RequestBody ConditionalOrders.Ticket ticket,HttpServletRequest request){var u=accounts.user(auth.getName());events.limit("CONDITIONAL_ORDER",auth.getName(),request,30,100,u.getId());try{return service.submit(auth.getName(),ticket);}catch(RuntimeException failure){events.record("ORDER_REJECTED",u.getId(),auth.getName(),request,"Conditional order rejected: "+failure.getClass().getSimpleName());throw failure;}}
    @DeleteMapping("/{id}") public Map<String,String> cancel(Authentication auth,@PathVariable String id){service.cancel(auth.getName(),id);return Map.of("message","Pending order cancelled.");}
}
