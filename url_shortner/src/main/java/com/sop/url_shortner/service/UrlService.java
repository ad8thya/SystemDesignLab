package com.sop.url_shortner.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sop.url_shortner.repository.UrlRepository;

@Service
public class UrlService {

    @Autowired
    private UrlRepository repo;

    public String convertToShort(String longurl) {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(longurl.getBytes(StandardCharsets.UTF_8));

        String hex = java.util.HexFormat.of().formatHex(hash);
        if ()

    }
    
}
