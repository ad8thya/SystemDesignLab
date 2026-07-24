package com.sop.url_shortner.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sop.url_shortner.model.UrlEntity;

public interface UrlRepository extends MongoRepository<UrlEntity, String> {
    
    Optional<UrlEntity> findByShortUrl(String shortUrl);
    Optional<UrlEntity> findByLongUrl(String longUrl);

    boolean existsByShortUrl(String shortUrl);

}
