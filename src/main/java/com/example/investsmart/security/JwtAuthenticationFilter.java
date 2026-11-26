package com.example.investsmart.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final SecurityAccounts accounts;
    public JwtAuthenticationFilter(SecurityAccounts accounts){this.accounts=accounts;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        if(request.getCookies()!=null)for(Cookie cookie:request.getCookies())if(SecurityAccounts.COOKIE.equals(cookie.getName())){
            try{var user=accounts.authenticate(cookie.getValue(),request);if(user!=null)SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.getEmail(),null,List.of(new SimpleGrantedAuthority("ROLE_"+user.getRole()))));}
            catch(io.jsonwebtoken.JwtException|IllegalArgumentException invalid){SecurityContextHolder.clearContext();}
            break;
        }
        chain.doFilter(request,response);
    }
}
