package com.example.investsmart.config;

import com.example.investsmart.security.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.*;
import org.springframework.web.cors.*;

@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication(type=org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http,JwtAuthenticationFilter filter,SecurityEvents events,
        @Value("${investsmart.cors.origins:http://localhost:5173,http://127.0.0.1:5173}") String origins,@Value("${investsmart.security.secure-cookie:false}") boolean secure)throws Exception{
        var cors=new CorsConfiguration();cors.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s->!s.isEmpty()).toList());cors.setAllowCredentials(true);cors.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));cors.setAllowedHeaders(List.of("Content-Type","X-XSRF-TOKEN"));cors.setExposedHeaders(List.of("Retry-After"));var source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/**",cors);
        var csrf=CookieCsrfTokenRepository.withHttpOnlyFalse();csrf.setCookieCustomizer(builder->builder.path("/").sameSite("Lax").secure(secure));
        http.cors(c->c.configurationSource(source)).csrf(c->c.csrfTokenRepository(csrf).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
          .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(a->a
            .requestMatchers(HttpMethod.GET,"/health").permitAll()
            .requestMatchers("/auth/phone/**","/auth/csrf","/auth/login","/auth/register","/auth/verify-email","/auth/resend-verification","/auth/forgot-password","/auth/reset-password","/auth/mfa","/auth/logout","/auth/mail-status","/auth/lock-status","/auth/unlock/**").permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.POST,"/stocks/import").authenticated()
            .requestMatchers(HttpMethod.POST,"/stocks").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PUT,"/stocks/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE,"/stocks/**").hasRole("ADMIN")
            .requestMatchers("/users/me","/users/me/**").authenticated()
            .requestMatchers("/users","/users/**").denyAll()
            .anyRequest().authenticated())
          .exceptionHandling(e->e.authenticationEntryPoint((request,response,error)->{boolean locked=Boolean.TRUE.equals(request.getAttribute("sessionLocked"));response.setStatus(locked?423:401);response.setContentType("application/json");response.getWriter().write(locked?"{\"status\":423,\"message\":\"This app is locked. Use your trusted-device passkey or sign in again.\"}":"{\"status\":401,\"message\":\"Please sign in again.\"}");})
            .accessDeniedHandler((request,response,error)->{var auth=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();events.record("ACCESS_DENIED",(Long)request.getAttribute("userId"),auth==null?events.source(request):auth.getName(),request,"Protected operation denied");response.setStatus(403);response.setContentType("application/json");response.getWriter().write("{\"status\":403,\"message\":\"Access denied or security token missing. Refresh and retry.\"}");}))
          .addFilterBefore(filter,UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
