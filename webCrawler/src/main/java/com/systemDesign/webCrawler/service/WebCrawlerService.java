package com.systemDesign.webCrawler.service;

import com.systemDesign.webCrawler.model.CrawlRequest;
import com.systemDesign.webCrawler.model.CrawlResponse;
import com.systemDesign.webCrawler.model.PageMetadata;
import com.systemDesign.webCrawler.model.QueueItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebCrawlerService {

    private final RedisCrawlStateService redisCrawlStateService;
    private final UrlFilterService urlFilterService;

    @Value("${crawler.connect-timeout-ms:3000}")
    private int connectTimeoutMs;

    @Value("${crawler.user-agent:Mozilla/5.0 (compatible; SystemDesignLabCrawler/1.0)}")
    private String userAgent;

    public CrawlResponse crawl(CrawlRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Starting crawl with seed URL: {}, maxDepth: {}, maxPages: {}",
                request.getSeedUrl(), request.getMaxDepth(), request.getMaxPages());

        // 1. Reset Redis state if fresh crawl requested
        if (request.isFreshCrawl()) {
            redisCrawlStateService.resetAll();
        }

        redisCrawlStateService.setStatus("RUNNING");

        // 2. Prepare allowed domains set (O(1) lookups)
        Set<String> allowedDomains = new HashSet<>();
        if (request.getAllowedDomains() != null && !request.getAllowedDomains().isEmpty()) {
            for (String d : request.getAllowedDomains()) {
                allowedDomains.add(d.trim().toLowerCase(Locale.ROOT));
            }
        } else if (request.getSeedUrl() != null && !request.getSeedUrl().isBlank()) {
            try {
                String seedHost = URI.create(request.getSeedUrl()).getHost();
                if (seedHost != null) {
                    allowedDomains.add(seedHost.toLowerCase(Locale.ROOT));
                }
            } catch (Exception e) {
                log.warn("Unable to extract host from seed URL: {}", request.getSeedUrl());
            }
        }

        // 3. Normalize and enqueue seed URL at depth 0
        String normalizedSeed = urlFilterService.normalize(request.getSeedUrl());
        if (normalizedSeed == null || !urlFilterService.isValid(normalizedSeed, allowedDomains)) {
            redisCrawlStateService.setStatus("FAILED");
            return CrawlResponse.builder()
                    .status("FAILED")
                    .seedUrl(request.getSeedUrl())
                    .message("Seed URL is invalid or blocked by filters: " + request.getSeedUrl())
                    .build();
        }

        redisCrawlStateService.enqueue(normalizedSeed, 0);
        redisCrawlStateService.recordDiscoveredUrl(normalizedSeed);

        int pagesCrawled = 0;

        // 4. BFS Crawl Loop
        while (!redisCrawlStateService.isQueueEmpty() && pagesCrawled < request.getMaxPages()) {
            QueueItem current = redisCrawlStateService.dequeue();
            if (current == null) {
                break;
            }

            String currentUrl = current.getUrl();
            int currentDepth = current.getDepth();

            // Check if already visited via Redis Set (O(1) duplicate check)
            if (redisCrawlStateService.isVisited(currentUrl)) {
                log.debug("Skipping already visited URL: {}", currentUrl);
                continue;
            }

            // Mark URL as visited in Redis
            redisCrawlStateService.markVisited(currentUrl);

            // Fetch and parse the page
            PageMetadata pageMetadata = crawlPage(currentUrl, currentDepth, request.getMaxDepth(), allowedDomains);
            if (pageMetadata != null) {
                redisCrawlStateService.savePageMetadata(pageMetadata);
                pagesCrawled++;
            }
        }

        long durationMs = System.currentTimeMillis() - startTime;
        redisCrawlStateService.setStatus("COMPLETED");

        Set<String> visitedUrls = redisCrawlStateService.getVisitedUrls();
        Set<String> discoveredUrls = redisCrawlStateService.getDiscoveredUrls();
        List<PageMetadata> crawledPages = redisCrawlStateService.getAllCachedPages();

        log.info("Crawl completed: {} pages crawled, {} links discovered in {} ms",
                visitedUrls.size(), discoveredUrls.size(), durationMs);

        return CrawlResponse.builder()
                .status("COMPLETED")
                .seedUrl(request.getSeedUrl())
                .totalPagesCrawled(visitedUrls.size())
                .totalDiscoveredUrls(discoveredUrls.size())
                .durationMs(durationMs)
                .visitedUrls(visitedUrls)
                .discoveredUrls(discoveredUrls)
                .crawledPages(crawledPages)
                .message("Successfully crawled " + visitedUrls.size() + " pages.")
                .build();
    }

    private PageMetadata crawlPage(String url, int depth, int maxDepth, Set<String> allowedDomains) {
        long pageStart = System.currentTimeMillis();
        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(userAgent)
                    .timeout(connectTimeoutMs)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .execute();

            int statusCode = response.statusCode();
            Document doc = response.parse();
            String title = doc.title();

            List<String> outgoingUrls = new ArrayList<>();

            // Extract hyperlinks only if current depth < maxDepth
            Elements linkElements = doc.select("a[href]");
            for (Element link : linkElements) {
                String absHref = link.attr("abs:href");
                String normalizedLink = urlFilterService.normalize(absHref);

                if (normalizedLink != null && urlFilterService.isValid(normalizedLink, allowedDomains)) {
                    outgoingUrls.add(normalizedLink);
                    redisCrawlStateService.recordDiscoveredUrl(normalizedLink);

                    // Add to queue if within depth limit and not yet visited
                    if (depth < maxDepth && !redisCrawlStateService.isVisited(normalizedLink)) {
                        redisCrawlStateService.enqueue(normalizedLink, depth + 1);
                    }
                }
            }

            long latencyMs = System.currentTimeMillis() - pageStart;

            return PageMetadata.builder()
                    .url(url)
                    .title(title)
                    .httpStatus(statusCode)
                    .crawlDepth(depth)
                    .discoveredLinksCount(outgoingUrls.size())
                    .outgoingUrls(outgoingUrls)
                    .responseTimeMs(latencyMs)
                    .crawledAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - pageStart;
            log.warn("Error fetching URL {}: {}", url, e.getMessage());

            return PageMetadata.builder()
                    .url(url)
                    .title("Error: " + e.getMessage())
                    .httpStatus(500)
                    .crawlDepth(depth)
                    .discoveredLinksCount(0)
                    .outgoingUrls(Collections.emptyList())
                    .responseTimeMs(latencyMs)
                    .crawledAt(LocalDateTime.now())
                    .build();
        }
    }
}
