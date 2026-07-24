package com.sop.url_shortner.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.sop.url_shortner.model.UrlEntity;
import com.sop.url_shortner.repository.UrlRepository;

@Service
public class UrlService {

    @Autowired
    private UrlRepository repo;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;



    public String convertToShort(String longUrl) {

        // Check if the URL already exists
        Optional<UrlEntity> existing = repo.findByLongUrl(longUrl);

        if (existing.isPresent()) {
            return existing.get().getShortUrl();
        }

        // Generate a unique short code
        String shortCode = generateUniqueShortCode(longUrl);

        // Save it
        UrlEntity entity = new UrlEntity();
        entity.setLongUrl(longUrl);
        entity.setShortUrl(shortCode);

        repo.save(entity);

        return shortCode;
    }

    private String generateUniqueShortCode(String longUrl) {

        String hash = hash(longUrl);

        int length = 6;

        while (true) {

            String shortCode = hash.substring(0, length);

            if (!repo.existsByShortUrl(shortCode)) {
                return shortCode;
            }

            length++;

            // Fallback if the whole hash is somehow exhausted
            if (length > hash.length()) {
                shortCode = hash + System.currentTimeMillis();

                if (!repo.existsByShortUrl(shortCode)) {
                    return shortCode;
                }
            }
        }
    }

    private String hash(String longUrl) {

        try {

            MessageDigest md = MessageDigest.getInstance("SHA-256");

            byte[] hash = md.digest(longUrl.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {

            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public String getLongUrl(String shortCode) {

    // 1. Check Redis first
        String longUrl = redisTemplate.opsForValue().get(shortCode);

        if (longUrl != null) {
            System.out.println("Redis Hit");
            return longUrl;
        }

        System.out.println("Redis Miss");

        // 2. Check MongoDB
        Optional<UrlEntity> url = repo.findByShortUrl(shortCode);

        if (url.isEmpty()) {
            return null;
        }

        // 3. Store in Redis
        longUrl = url.get().getLongUrl();

        redisTemplate.opsForValue().set(shortCode, longUrl);

        // OR (recommended)
        // redisTemplate.opsForValue().set(shortCode, longUrl,
        //         Duration.ofHours(1));

        return longUrl;
    }
        

}
