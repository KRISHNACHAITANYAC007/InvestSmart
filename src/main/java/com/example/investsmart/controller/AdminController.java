package com.example.investsmart.controller;
import com.example.investsmart.security.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.example.investsmart.investment.IpoMarketService;
import com.example.investsmart.investment.BondMarketService;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/admin")
public class AdminController {
    private final AdminService admin;private final SecurityAccounts accounts;
    private final AccountMail mail;
    public AdminController(AdminService admin,SecurityAccounts accounts,AccountMail mail){this.admin=admin;this.accounts=accounts;this.mail=mail;}
    public record Action(@NotBlank @Size(max=30) String action,@NotBlank @Size(min=5,max=1000) String reason){}
    public record Review(@NotBlank String status,@NotBlank @Size(min=5,max=1000) String notes){}
    public record DeleteUser(@NotBlank @Size(max=254) String confirmation,@Size(max=1000) String reason){}
    public record Reason(@NotBlank @Size(min=5,max=1000) String reason){}
    public record BalanceAdjustment(@NotNull @DecimalMin(value="-1000000.00") @DecimalMax(value="1000000.00") BigDecimal amount,@NotBlank @Size(min=5,max=1000) String reason){}
    public record ListingUpdate(@NotBlank String type,@NotBlank @Size(max=180) String referenceId,@NotNull Boolean available,
                                LocalDate openDate,LocalDate closeDate,LocalDate allotmentDate,LocalDate listingDate,
                                @NotBlank @Size(min=5,max=1000) String reason){}
    public record SettlementRetry(@NotBlank @Size(min=5,max=1000) String reason){}
    @GetMapping("/overview") public Map<String,Object> overview(){return admin.overview();}
    @GetMapping("/users") public AdminService.Page users(@RequestParam(defaultValue="") @Size(max=100) String query,@RequestParam(defaultValue="ALL") String status,@RequestParam(defaultValue="1") int page){return admin.users(query,status,page);}
    @GetMapping("/users/{id}") public Map<String,Object> detail(Principal p,@PathVariable Long id){return admin.detail(accounts.user(p.getName()).getId(),id);}
    @PostMapping("/users/{id}/actions") public void action(Principal p,@PathVariable Long id,@Valid @RequestBody Action body,HttpServletRequest request){admin.accountAction(p.getName(),id,body.action(),body.reason(),request);}
    @PostMapping("/users/{id}/password-reset") public Map<String,String> passwordReset(Principal p,@PathVariable Long id,@Valid @RequestBody Reason body,HttpServletRequest request){long mailId=admin.requestPasswordReset(p.getName(),id,body.reason(),request);mail.deliver(mailId);return Map.of("message","Password-reset email queued and existing sessions revoked. Email delivery is not confirmed.");}
    @PostMapping("/users/{id}/resend-verification") public Map<String,String> resendVerification(Principal p,@PathVariable Long id,HttpServletRequest request){long mailId=admin.resendVerification(p.getName(),id,request);mail.deliver(mailId);return Map.of("message","Verification email queued. Delivery and inbox receipt are not confirmed.");}
    @DeleteMapping("/users/{id}/sessions/{sessionId}") public void revokeSession(Principal p,@PathVariable Long id,@PathVariable String sessionId,@Valid @RequestBody Reason body,HttpServletRequest request){admin.revokeSession(p.getName(),id,sessionId,body.reason(),request);}
    @PostMapping("/users/{id}/balance-adjustments") public void adjustBalance(Principal p,@PathVariable Long id,@Valid @RequestBody BalanceAdjustment body,HttpServletRequest request){admin.adjustBalance(p.getName(),id,body.amount(),body.reason(),request);}
    @DeleteMapping("/users/{id}") public Map<String,Object> deleteUser(Principal p,@PathVariable Long id,@Valid @RequestBody DeleteUser body,HttpServletRequest request){long mailId=admin.deleteUser(p.getName(),id,body.confirmation(),body.reason(),request);if(mailId>0)mail.deliver(mailId);return admin.overview();}
    @GetMapping("/alerts") public AdminService.Page alerts(@RequestParam(defaultValue="OPEN") String status,@RequestParam(defaultValue="1") int page){return admin.alerts(status,page);}
    @GetMapping("/alerts/{id}") public Map<String,Object> alert(@PathVariable Long id){return admin.alert(id);}
    @PostMapping("/alerts/{id}/review") public void review(Principal p,@PathVariable Long id,@Valid @RequestBody Review body,HttpServletRequest request){admin.review(accounts.user(p.getName()).getId(),id,body.status(),body.notes(),request);}
    @GetMapping("/audit") public AdminService.Page audit(@RequestParam(defaultValue="1") int page){return admin.audit(page);}
    @GetMapping("/operations") public Map<String,Object> operations(@RequestParam(defaultValue="ALL") String status,@RequestParam(defaultValue="1") int page){return admin.operations(status,page);}
    @GetMapping("/operations/listings/ipos") public IpoMarketService.Page<IpoMarketService.Ipo> ipoListings(@RequestParam(defaultValue="open") String status,@RequestParam(defaultValue="all") String issueType,@RequestParam(defaultValue="1") int page){return admin.ipoListings(status,issueType,page);}
    @GetMapping("/operations/listings/bonds") public BondMarketService.BondPage bondListings(@RequestParam(defaultValue="all") String category,@RequestParam(defaultValue="") @Size(max=80) String query,@RequestParam(defaultValue="1") int page){return admin.bondListings(category,query,page);}
    @PostMapping("/operations/listings") public AdminListingOverrides.Override updateListing(Principal p,@Valid @RequestBody ListingUpdate body,HttpServletRequest request){return admin.updateListing(p.getName(),body.type(),body.referenceId(),body.available(),body.openDate(),body.closeDate(),body.allotmentDate(),body.listingDate(),body.reason(),request);}
    @PostMapping("/operations/ipo-applications/{id}/settlement-retry") public Map<String,Object> retrySettlement(Principal p,@PathVariable Long id,@Valid @RequestBody SettlementRetry body,HttpServletRequest request){return admin.retryIpoSettlement(p.getName(),id,body.reason(),request);}
}
