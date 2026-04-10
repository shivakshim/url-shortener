package com.project2.urlshortner.rateLimiter;

import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimiterInterceptor implements HandlerInterceptor {

    @Autowired
    private RateLimiterClient rateLimiterClient;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        System.out.println("🔥 INTERCEPTOR HIT: " + request.getRequestURI());

        String clientId = request.getHeader("X-Forwarded-For");
        if (clientId == null) {
            clientId = request.getRemoteAddr();
        }

        String endpoint = "/redirect";

        System.out.println("➡️ Checking RL for client: " + clientId);

        boolean allowed = rateLimiterClient.isAllowed(clientId, endpoint);

        if (!allowed) {
            System.out.println("🚫 BLOCKING REQUEST");

            response.setStatus(429);
            response.getWriter().write("Too Many Requests");
            return false;
        }

        System.out.println("✅ REQUEST ALLOWED");
        return true;
    }
}
