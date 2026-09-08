package com.systemDesign.webCrawler.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlResponse {
    private String status;
    private String seedUrl;
    private int totalPagesCrawled;
    private int totalDiscoveredUrls;
    private long durationMs;
    private Set<String> visitedUrls;
    private Set<String> discoveredUrls;
    private List<PageMetadata> crawledPages;
    private String message;
}
