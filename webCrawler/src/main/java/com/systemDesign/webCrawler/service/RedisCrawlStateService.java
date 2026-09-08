package com.systemDesign.webCrawler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemDesign.webCrawler.model.PageMetadata;
import com.systemDesign.webCrawler.model.QueueItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCrawlStateService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public static final String KEY_QUEUE = "crawler:queue";
    public static final String KEY_VISITED = "crawler:visited";
    public static final String KEY_DISCOVERED = "crawler:discovered";
    public static final String KEY_PAGES = "crawler:pages";
    public static final String KEY_STATUS = "crawler:status";

    // ----------------------------------------------------
    // 1. URL Queue Management (Redis LIST: FIFO Queue)
    // ----------------------------------------------------
    public void enqueue(String url, int depth) {
        QueueItem item = new QueueItem(url, depth);
        redisTemplate.opsForList().rightPush(KEY_QUEUE, item);
    }

    public QueueItem dequeue() {
        Object item = redisTemplate.opsForList().leftPop(KEY_QUEUE);
        if (item == null) {
            return null;
        }
        return objectMapper.convertValue(item, QueueItem.class);
    }

    public boolean isQueueEmpty() {
        Long size = redisTemplate.opsForList().size(KEY_QUEUE);
        return size == null || size == 0;
    }

    public long getQueueSize() {
        Long size = redisTemplate.opsForList().size(KEY_QUEUE);
        return size != null ? size : 0;
    }

    // ----------------------------------------------------
    // 2. Visited-URL Set Management (Redis SET: O(1) deduplication)
    // ----------------------------------------------------
    public boolean isVisited(String url) {
        Boolean member = redisTemplate.opsForSet().isMember(KEY_VISITED, url);
        return Boolean.TRUE.equals(member);
    }

    public boolean markVisited(String url) {
        Long added = redisTemplate.opsForSet().add(KEY_VISITED, url);
        return added != null && added > 0;
    }

    public Set<String> getVisitedUrls() {
        Set<Object> members = redisTemplate.opsForSet().members(KEY_VISITED);
        if (members == null) {
            return Collections.emptySet();
        }
        return members.stream().map(Object::toString).collect(Collectors.toSet());
    }

    public long getVisitedCount() {
        Long size = redisTemplate.opsForSet().size(KEY_VISITED);
        return size != null ? size : 0;
    }

    // ----------------------------------------------------
    // 3. Discovered URLs Set Management
    // ----------------------------------------------------
    public void recordDiscoveredUrl(String url) {
        redisTemplate.opsForSet().add(KEY_DISCOVERED, url);
    }

    public void recordDiscoveredUrls(Collection<String> urls) {
        if (urls != null && !urls.isEmpty()) {
            redisTemplate.opsForSet().add(KEY_DISCOVERED, urls.toArray());
        }
    }

    public Set<String> getDiscoveredUrls() {
        Set<Object> members = redisTemplate.opsForSet().members(KEY_DISCOVERED);
        if (members == null) {
            return Collections.emptySet();
        }
        return members.stream().map(Object::toString).collect(Collectors.toSet());
    }

    // ----------------------------------------------------
    // 4. Page Metadata Cache (Redis HASH)
    // ----------------------------------------------------
    public void savePageMetadata(PageMetadata metadata) {
        redisTemplate.opsForHash().put(KEY_PAGES, metadata.getUrl(), metadata);
    }

    public PageMetadata getPageMetadata(String url) {
        Object obj = redisTemplate.opsForHash().get(KEY_PAGES, url);
        if (obj == null) {
            return null;
        }
        return objectMapper.convertValue(obj, PageMetadata.class);
    }

    public List<PageMetadata> getAllCachedPages() {
        List<Object> values = redisTemplate.opsForHash().values(KEY_PAGES);
        if (values == null) {
            return Collections.emptyList();
        }
        return values.stream()
            .map(val -> objectMapper.convertValue(val, PageMetadata.class))
            .collect(Collectors.toList());
    }

    // ----------------------------------------------------
    // 5. Crawl Status & Reset
    // ----------------------------------------------------
    public void setStatus(String status) {
        redisTemplate.opsForValue().set(KEY_STATUS, status);
    }

    public String getStatus() {
        Object status = redisTemplate.opsForValue().get(KEY_STATUS);
        return status != null ? status.toString() : "IDLE";
    }

    public void resetAll() {
        redisTemplate.delete(List.of(KEY_QUEUE, KEY_VISITED, KEY_DISCOVERED, KEY_PAGES, KEY_STATUS));
        log.info("Cleared all crawler Redis keys.");
    }
}
