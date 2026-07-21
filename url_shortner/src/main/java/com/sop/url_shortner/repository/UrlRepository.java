package com.sop.url_shortner.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sop.url_shortner.model.UrlEntity;

public interface UrlRepository extends MongoRepository<String, UrlEntity> {
    
}
