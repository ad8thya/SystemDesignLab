package com.systemDesign.webCrawler.controller;

import com.systemDesign.webCrawler.model.CrawlRequest;
import com.systemDesign.webCrawler.model.CrawlResponse;
import com.systemDesign.webCrawler.service.RedisCrawlStateService;
import com.systemDesign.webCrawler.service.WebCrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
public class WebCrawlerController {

    private final WebCrawlerService webCrawlerService;
    private final RedisCrawlStateService redisCrawlStateService;

    /**
     * Trigger a crawl using POST with a JSON body.
     */
    @PostMapping("/crawl")
    public ResponseEntity<CrawlResponse> crawlPost(@RequestBody(required = false) CrawlRequest request) {
        if (request == null) {
            request = CrawlRequest.builder()
                    .seedUrl("http://localhost:8080/mock-web/home")
                    .maxDepth(2)
                    .maxPages(20)
                    .freshCrawl(true)
                    .build();
        }
        CrawlResponse response = webCrawlerService.crawl(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Convenience GET endpoint for easy browser and JMeter load testing.
     */
    @GetMapping("/crawl")
    public ResponseEntity<CrawlResponse> crawlGet(
            @RequestParam(defaultValue = "http://localhost:8080/mock-web/home") String seedUrl,
            @RequestParam(defaultValue = "2") int maxDepth,
            @RequestParam(defaultValue = "20") int maxPages,
            @RequestParam(defaultValue = "true") boolean freshCrawl,
            @RequestParam(required = false) Set<String> allowedDomains) {

        CrawlRequest request = CrawlRequest.builder()
                .seedUrl(seedUrl)
                .maxDepth(maxDepth)
                .maxPages(maxPages)
                .allowedDomains(allowedDomains)
                .freshCrawl(freshCrawl)
                .build();

        CrawlResponse response = webCrawlerService.crawl(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Reset Redis crawler state.
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset() {
        redisCrawlStateService.resetAll();
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Redis crawler queue, visited set, and page cache have been reset."
        ));
    }
}
