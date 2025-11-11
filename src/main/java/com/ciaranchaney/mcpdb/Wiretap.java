package com.ciaranchaney.mcpdb;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.stereotype.Component;

@Component
public class Wiretap implements jakarta.servlet.Filter {
    @Override public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws java.io.IOException, jakarta.servlet.ServletException {
        var r = (jakarta.servlet.http.HttpServletRequest) req;
        System.out.printf("[DISCOVERY] %s %s Accept=%s%n", r.getMethod(), r.getRequestURI(), r.getHeader("Accept"));
        chain.doFilter(req, res);
    }
}
