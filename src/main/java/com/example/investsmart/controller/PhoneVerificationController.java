package com.example.investsmart.controller;

import com.example.investsmart.security.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
public class PhoneVerificationController {
    private final PhoneVerification phone;
    private final SecurityAccounts accounts;
    public PhoneVerificationController(PhoneVerification phone,SecurityAccounts accounts){this.phone=phone;this.accounts=accounts;}
    public record Token(@NotBlank @Size(max=200) String token){}
    public record Challenge(@NotBlank @Size(max=200) String challenge){}
    public record Code(@NotBlank @Size(max=200) String challenge,@NotBlank @Size(max=100) String code){}
    public record Credential(@NotBlank @Size(max=200) String challenge,@NotBlank @Size(max=12000) String credential,@Size(max=200) String loginChallenge){}
    public record Replacement(@NotBlank @Size(max=100) String password,@NotBlank @Size(max=100) String code){}
    @ModelAttribute public void headers(HttpServletResponse response){response.setHeader("Cache-Control","no-store");response.setHeader("Pragma","no-cache");response.setHeader("Referrer-Policy","no-referrer");}
    @PostMapping("/auth/phone/pair") public void pair(@Valid @RequestBody Token body,HttpServletResponse response){phone.openPairing(body.token(),response);}
    @PostMapping("/auth/phone/replacement") public void openReplacement(@Valid @RequestBody Token body,HttpServletResponse response){phone.openReplacement(body.token(),response);}
    @PostMapping("/auth/phone/registration/options") public PhoneVerification.Ceremony registration(HttpServletRequest request){return phone.registrationOptions(request);}
    @PostMapping("/auth/phone/registration") public PhoneVerification.Setup register(@Valid @RequestBody Credential body,HttpServletRequest request){return phone.register(body.challenge(),body.credential(),request);}
    @PostMapping("/auth/phone/acknowledge") public void acknowledge(HttpServletRequest request,HttpServletResponse response){phone.acknowledge(request,response);}
    @PostMapping("/auth/phone/display/options") public PhoneVerification.Ceremony displayOptions(HttpServletRequest request){return phone.displayOptions(request);}
    @PostMapping("/auth/phone/display") public void display(@Valid @RequestBody Credential body,HttpServletRequest request,HttpServletResponse response){phone.openDisplay(body.challenge(),body.credential(),request,response);}
    @GetMapping("/auth/phone/code") public PhoneVerification.Code code(HttpServletRequest request){return phone.code(request);}
    @PostMapping("/auth/phone/close") public void close(HttpServletRequest request,HttpServletResponse response){phone.close(request,response);}
    @PostMapping("/auth/phone/verify") public SecurityAccounts.LoginResult verify(@Valid @RequestBody Code body,HttpServletRequest request,HttpServletResponse response){return accounts.completePhone(body.challenge(),body.code(),request,response);}
    @PostMapping("/auth/phone/login/options") public PhoneVerification.Ceremony loginOptions(@Valid @RequestBody Challenge body,HttpServletRequest request){return phone.loginOptions(body.challenge(),request);}
    @PostMapping("/auth/phone/login") public SecurityAccounts.LoginResult login(@Valid @RequestBody Credential body,HttpServletRequest request,HttpServletResponse response){return accounts.completePhonePasskey(body.challenge(),body.credential(),body.loginChallenge(),request,response);}
    @PostMapping("/auth/phone/recover") public Map<String,String> recover(@Valid @RequestBody Code body,HttpServletRequest request){return Map.of("pairingUrl",phone.recover(body.challenge(),body.code(),request));}
    @GetMapping("/users/me/phone") public Map<String,Object> status(Principal principal){return phone.status(accounts.user(principal.getName()));}
    @PostMapping("/users/me/phone/replace") public Map<String,String> replace(Principal principal,@Valid @RequestBody Replacement body,HttpServletRequest request){return Map.of("pairingUrl",phone.replace(accounts.user(principal.getName()),body.password(),body.code(),request));}
}
