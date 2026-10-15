package com.rateLimiter.demo.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ratelimiter")
public class RateLimiterProperties {

    private long capacity;
    private double refillRate;

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public double getRefillRate() {
        return refillRate;
    }

    public void setRefillRate(double refillRate) {
        this.refillRate = refillRate;
    }
}