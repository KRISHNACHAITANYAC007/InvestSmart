package com.example.investsmart.controller;
import com.example.investsmart.security.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/users/me")
public class UserController {
    private final SecurityAccounts accounts;
    public UserController(SecurityAccounts accounts){this.accounts=accounts;}
    public record PasswordRequest(@NotBlank @Size(max=100) String currentPassword,@NotBlank @Size(max=100) String newPassword){}
    public record ReauthRequest(@NotBlank @Size(max=100) String password,@NotBlank @Size(max=100) String code){}
    @GetMapping public SecurityAccounts.Account me(Principal p){return accounts.view(accounts.user(p.getName()));}
    @GetMapping("/sessions") public List<Map<String,Object>> sessions(Principal p,HttpServletRequest request){return accounts.sessions(accounts.user(p.getName()),(String)request.getAttribute("sessionId"));}
    @DeleteMapping("/sessions/{id}") public void revoke(Principal p,@PathVariable String id){accounts.revokeSession(accounts.user(p.getName()).getId(),id);}
    @PostMapping("/sessions/revoke-others") public void revokeOthers(Principal p,HttpServletRequest request){accounts.revokeOtherSessions(accounts.user(p.getName()).getId(),(String)request.getAttribute("sessionId"));}
    @PostMapping("/password") public void password(Principal p,@Valid @RequestBody PasswordRequest body,HttpServletRequest request,HttpServletResponse response){accounts.changePassword(p.getName(),body.currentPassword(),body.newPassword(),request);accounts.logout(request,response);}
    @PostMapping("/reauthenticate") public void reauth(Principal p,@Valid @RequestBody ReauthRequest body,HttpServletRequest request){accounts.reauthenticate(p.getName(),body.password(),body.code(),request);}
}
