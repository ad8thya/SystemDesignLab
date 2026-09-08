package com.systemDesign.webCrawler.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlRequest {
    private String seedUrl;

    @Builder.Default
    private int maxDepth = 2;

    @Builder.Default
    private int maxPages = 20;

    private Set<String> allowedDomains;

    @Builder.Default
    private boolean freshCrawl = true;
}
