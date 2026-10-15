package com.rateLimiter.demo.interceptor;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.rateLimiter.demo.service.TokenBucketRateLimiter;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final TokenBucketRateLimiter rateLimiter;

    public RateLimitInterceptor(TokenBucketRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientId = request.getHeader("X-Client-Id");
        if (clientId == null || clientId.isBlank()) {
            clientId = request.getRemoteAddr();
        }

        if (rateLimiter.isAllowed(clientId)) {
            return true;
        }

        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Too Many Requests\"}");
        return false;
    }
}