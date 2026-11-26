package com.example.investsmart.trading;
import com.example.investsmart.security.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/orders/paper")
public class PaperController {
    private final PaperTrading trading;private final SecurityAccounts accounts;private final SecurityEvents events;
    public PaperController(PaperTrading trading,SecurityAccounts accounts,SecurityEvents events){this.trading=trading;this.accounts=accounts;this.events=events;}
    @PostMapping("/preview") public Map<String,Object> preview(Authentication auth,@RequestBody PaperTrading.Ticket ticket,HttpServletRequest request){var user=accounts.user(auth.getName());events.limit("PAPER_PREVIEW",auth.getName(),request,120,400,user.getId());return trading.preview(auth.getName(),ticket);}
    @PostMapping public Map<String,Object> submit(Authentication auth,@RequestBody PaperTrading.Ticket ticket,HttpServletRequest request){var user=accounts.user(auth.getName());events.limit("PAPER_ORDER",auth.getName(),request,30,100,user.getId());try{return trading.submit(auth.getName(),ticket);}catch(RuntimeException failure){events.record("ORDER_REJECTED",user.getId(),auth.getName(),request,"Paper ticket rejected: "+failure.getClass().getSimpleName());throw failure;}}
    @GetMapping public Map<String,Object> account(Authentication auth){return trading.account(auth.getName());}
    @DeleteMapping("/{id}") public Map<String,String> cancel(Authentication auth,@PathVariable String id){trading.cancel(auth.getName(),id);return Map.of("message","Unfilled reservations released.");}
    @PostMapping("/positions/{id}/close") public Map<String,String> close(Authentication auth,@PathVariable String id){trading.requestClose(auth.getName(),id);return Map.of("message","Closure requested; fresh quote and open market required.");}
    @PutMapping("/{id}") public Map<String,Object> edit(Authentication auth,@PathVariable String id,@RequestBody PaperTrading.Ticket ticket){return trading.edit(auth.getName(),id,ticket);}
}
