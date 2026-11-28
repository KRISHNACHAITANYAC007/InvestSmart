package com.example.investsmart.reports;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Runs before authentication so denied financial-report requests also prohibit caching. */
@Component @Order(Ordered.HIGHEST_PRECEDENCE+10)
public class ReportNoStoreFilter extends OncePerRequestFilter {
    @Override protected boolean shouldNotFilter(HttpServletRequest request){return !request.getRequestURI().equals(request.getContextPath()+"/reports/journal-period");}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        response.setHeader("Cache-Control","no-store");response.setHeader("Pragma","no-cache");chain.doFilter(request,response);
    }
}
