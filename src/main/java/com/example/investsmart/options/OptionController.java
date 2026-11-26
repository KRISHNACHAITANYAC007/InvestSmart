package com.example.investsmart.options;

import java.time.LocalDate;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.investsmart.security.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController @RequestMapping("/options")
public class OptionController {
    private final OptionMarket market;private final OptionTrading trading;private final SecurityAccounts accounts;private final SecurityEvents events;
    public OptionController(OptionMarket market,OptionTrading trading,SecurityAccounts accounts,SecurityEvents events){this.market=market;this.trading=trading;this.accounts=accounts;this.events=events;}
    @GetMapping("/contracts") public List<OptionMarket.Contract> contracts(){return market.contracts();}
    @GetMapping("/chain") public List<OptionMarket.Quote> chain(@RequestParam LocalDate expiry){var chain=market.chain(expiry);trading.mark(chain);return chain;}
    @GetMapping("/account") public Map<String,Object> account(Authentication auth){return trading.account(auth.getName());}
    @PostMapping("/orders") public Map<String,Object> submit(Authentication auth,@RequestBody OptionTrading.Ticket ticket,HttpServletRequest request){
        var user=accounts.user(auth.getName());events.limit("OPTION_ORDER",auth.getName(),request,30,100,user.getId());
        try{return OptionTrading.view(trading.submit(auth.getName(),ticket));}catch(RuntimeException rejected){events.record("ORDER_REJECTED",user.getId(),auth.getName(),request,"Option order rejected: "+rejected.getClass().getSimpleName());throw rejected;}
    }
    @DeleteMapping("/orders/{id}") public Map<String,String> cancel(Authentication auth,@PathVariable String id){trading.cancel(auth.getName(),id);return Map.of("message","Pending order cancelled; reservations released.");}
}
