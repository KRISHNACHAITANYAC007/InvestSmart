package com.example.investsmart.controller;
import com.example.investsmart.security.SecurityAccounts;
import com.example.investsmart.dto.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.security.Principal;

@RestController @RequestMapping("/auth")
public class AuthController {
    private final SecurityAccounts accounts;
    private final com.example.investsmart.security.AccountMail mail;
    private final com.example.investsmart.security.PasskeyService passkeys;
    public AuthController(SecurityAccounts accounts,com.example.investsmart.security.AccountMail mail,com.example.investsmart.security.PasskeyService passkeys){this.accounts=accounts;this.mail=mail;this.passkeys=passkeys;}
    public record EmailRequest(@NotBlank @Email @Size(max=254) String email){}
    public record TokenRequest(@NotBlank @Size(max=200) String token){}
    public record ResetRequest(@NotBlank @Size(max=200) String token,@NotBlank @Size(max=100) String password){}
    public record MfaRequest(@NotBlank @Size(max=200) String challenge,@NotBlank @Size(max=100) String code){}
    public record PasskeyResponseRequest(@NotBlank @Size(max=200) String challenge,@NotBlank @Size(max=12000) String credential){}
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken());}
    @PostMapping("/login") public SecurityAccounts.LoginResult login(@Valid @RequestBody LoginRequest body,HttpServletRequest request,HttpServletResponse response){var result=accounts.login(body.getEmail(),body.getPassword(),request,response);Object mailId=request.getAttribute("phoneMailId");if(mailId instanceof Long id)deliver(id);return result;}
    @PostMapping("/register") public Map<String,String> register(@Valid @RequestBody RegisterRequest body,HttpServletRequest request){deliver(accounts.register(body.getName(),body.getEmail(),body.getPassword(),request));return accepted("VERIFY");}
    @PostMapping("/resend-verification") public Map<String,String> resend(@Valid @RequestBody EmailRequest body,HttpServletRequest request){deliver(accounts.requestMail(body.email(),"VERIFY",request));return accepted("VERIFY");}
    @PostMapping("/forgot-password") public Map<String,String> forgot(@Valid @RequestBody EmailRequest body,HttpServletRequest request){deliver(accounts.requestMail(body.email(),"RESET",request));return accepted("RESET");}
    @PostMapping("/verify-email") public Map<String,String> verify(@Valid @RequestBody TokenRequest body){accounts.verify(body.token());return Map.of("message","Email verified. You can now sign in.");}
    @PostMapping("/reset-password") public Map<String,String> reset(@Valid @RequestBody ResetRequest body){accounts.reset(body.token(),body.password());return Map.of("message","Password updated. Sign in with your new password.");}
    @PostMapping("/mfa") public SecurityAccounts.LoginResult mfa(@Valid @RequestBody MfaRequest body,HttpServletRequest request,HttpServletResponse response){return accounts.completeMfa(body.challenge(),body.code(),request,response);}
    @PostMapping("/logout") public void logout(HttpServletRequest request,HttpServletResponse response){accounts.logout(request,response);}
    @GetMapping("/mail-status") public Map<String,Object> mailStatus(){return mail.status();}
    @GetMapping("/lock-status") public Map<String,Object> lockStatus(HttpServletRequest request){return passkeys.lockStatus(request);}
    @PostMapping("/lock") public void lock(Principal principal,HttpServletRequest request){passkeys.lock(accounts.user(principal.getName()),request);}
    @PostMapping("/unlock/options") public com.example.investsmart.security.PasskeyService.Ceremony unlockOptions(HttpServletRequest request){return passkeys.unlockOptions(request);}
    @PostMapping("/unlock") public SecurityAccounts.Account unlock(@Valid @RequestBody PasskeyResponseRequest body,HttpServletRequest request){return passkeys.unlock(body.challenge(),body.credential(),request);}
    @GetMapping("/passkeys") public java.util.List<com.example.investsmart.security.PasskeyService.Passkey> passkeys(Principal principal){return passkeys.list(accounts.user(principal.getName()));}
    @PostMapping("/passkeys/registration/options") public com.example.investsmart.security.PasskeyService.Ceremony registerPasskeyOptions(Principal principal,HttpServletRequest request){return passkeys.registrationOptions(accounts.user(principal.getName()),request);}
    @PostMapping("/passkeys/registration") public Map<String,String> registerPasskey(Principal principal,@Valid @RequestBody PasskeyResponseRequest body,HttpServletRequest request){passkeys.finishRegistration(accounts.user(principal.getName()),body.challenge(),body.credential(),request);return Map.of("message","Passkey enabled for this account. Locking this app keeps the session active; signing out revokes it.");}
    @DeleteMapping("/passkeys/{id}") public void removePasskey(Principal principal,@PathVariable String id,HttpServletRequest request){passkeys.remove(accounts.user(principal.getName()),id,request);}
    private void deliver(long id){if(id>0)mail.deliver(id);}
    private Map<String,String> accepted(String purpose){return Map.of("message","VERIFY".equals(purpose)
            ?"If this address is eligible, a verification email has been queued. The link expires after 24 hours; check your inbox and spam folder. Unverified accounts cannot sign in."
            :"If this address is eligible, a password-reset email has been queued. The link expires after 30 minutes; check your inbox and spam folder.");}
}
