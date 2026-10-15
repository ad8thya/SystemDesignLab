package com.rateLimiter.demo.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    @GetMapping("/api/ping")
    public String ping() {
        return "pong";
    }
}