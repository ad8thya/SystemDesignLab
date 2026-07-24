package com.sop.url_shortner.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sop.url_shortner.repository.UrlRepository;
import com.sop.url_shortner.service.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.sop.url_shortner.dto.RequestPostDTO;
import com.sop.url_shortner.dto.ResponsePostDTO;
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

    @PostMapping("/shorten")
    public ResponsePostDTO longToShort(@RequestBody RequestPostDTO request) {

        String shortUrl = service.convertToShort(request.getLongUrl());

        ResponsePostDTO response = new ResponsePostDTO();
        response.setShortUrl("http://localhost:8080/url/"+shortUrl);

        return response;
    }

    @GetMapping("/{shorturl}")
    public ResponseEntity<Void> redirect(@PathVariable String shorturl) {

        String url = service.getLongUrl(shorturl);

        if (url == null){
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.status(302)
                .location(URI.create(url))
                .build();
    }
    
    

}
