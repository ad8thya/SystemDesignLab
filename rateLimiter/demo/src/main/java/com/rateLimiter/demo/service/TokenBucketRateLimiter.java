package com.rateLimiter.demo.service;


import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.rateLimiter.demo.config.RateLimiterProperties;

import java.util.Collections;

@Service
public class TokenBucketRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final RateLimiterProperties properties;
    private final DefaultRedisScript<Long> script;

    public TokenBucketRateLimiter(StringRedisTemplate redisTemplate, RateLimiterProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.script = new DefaultRedisScript<>();
        this.script.setLocation(new ClassPathResource("scripts/token_bucket.lua"));
        this.script.setResultType(Long.class);
    }

    public boolean isAllowed(String clientId) {
        String key = "rate_limit:" + clientId;
        long now = System.currentTimeMillis() / 1000;

        Long result = redisTemplate.execute(
                script,
                Collections.singletonList(key),
                String.valueOf(properties.getCapacity()),
                String.valueOf(properties.getRefillRate()),
                String.valueOf(now),
                "1"  // no of requested tokens
        );

        return result != null && result == 1L;
    }
}
