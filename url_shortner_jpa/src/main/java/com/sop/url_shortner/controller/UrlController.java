package com.sop.url_shortner.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sop.url_shortner.repository.UrlRepository;
import com.sop.url_shortner.service.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.sop.url_shortner.model.*;
import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/url")
public class UrlController {
    @Autowired
    private UrlService service;
    @Autowired
    private UrlRepository repo;

    @PostMapping("/shorten")
    public String longToShort(@RequestBody String longurl) {
        //TODO: process POST request
    
        return service.convertToShort(longurl);
    }

    @GetMapping("/{shorturl}")
    public ResponseEntity<Void> redirect(@PathVariable String shorturl) {

        UrlEntity url = repo.findByShortUrl(shorturl).orElse(null);

        if (url == null){
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.status(302)
                .location(URI.create(url.getLongUrl()))
                .build();
    }
    
    

}
