package com.sop.url_shortner.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sop.url_shortner.model.UrlEntity;

public interface UrlRepository extends JpaRepository<UrlEntity, Long> {
    
    Optional<UrlEntity> findByShortUrl(String shortUrl);
    Optional<UrlEntity> findByLongUrl(String longUrl);

    boolean existsByShortUrl(String shortUrl);

}
