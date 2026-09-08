package com.systemDesign.webCrawler.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageMetadata implements Serializable {
    private String url;
    private String title;
    private int httpStatus;
    private int crawlDepth;
    private int discoveredLinksCount;
    private List<String> outgoingUrls;
    private long responseTimeMs;
    private LocalDateTime crawledAt;
}
