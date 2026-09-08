package com.systemDesign.webCrawler.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

@Service
public class UrlFilterService {

    private static final Set<String> DISALLOWED_EXTENSIONS = Set.of(
        ".pdf", ".png", ".jpg", ".jpeg", ".gif", ".svg", ".ico",
        ".css", ".js", ".zip", ".tar", ".gz", ".rar", ".7z",
        ".mp4", ".mp3", ".avi", ".mov", ".wmv", ".flv",
        ".woff", ".woff2", ".ttf", ".eot", ".doc", ".docx", ".xls", ".xlsx"
    );

    public String normalize(String rawUrl) {
        if (rawUrl == null) {
            return null;
        }

        String url = rawUrl.trim();

        // Strip anchor fragments (e.g., http://example.com/page#top -> http://example.com/page)
        int hashIndex = url.indexOf('#');
        if (hashIndex != -1) {
            url = url.substring(0, hashIndex);
        }

        // Remove trailing slash if not root domain (e.g. /mock-web/about/ -> /mock-web/about)
        if (url.endsWith("/") && url.chars().filter(ch -> ch == '/').count() > 3) {
            url = url.substring(0, url.length() - 1);
        }

        return url;
    }

    public boolean isValid(String url, Set<String> allowedDomains) {
        if (url == null || url.isBlank()) {
            return false;
        }

        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (scheme == null) {
                return false;
            }

            scheme = scheme.toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) {
                return false;
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }

            // Check disallowed file extensions
            String path = uri.getPath();
            if (path != null) {
                String lowerPath = path.toLowerCase(Locale.ROOT);
                for (String ext : DISALLOWED_EXTENSIONS) {
                    if (lowerPath.endsWith(ext)) {
                        return false;
                    }
                }
            }

            // Check HashSet of allowed domains in O(1)
            if (allowedDomains != null && !allowedDomains.isEmpty()) {
                String lowerHost = host.toLowerCase(Locale.ROOT);
                boolean matches = allowedDomains.stream()
                    .anyMatch(domain -> lowerHost.equals(domain.toLowerCase(Locale.ROOT))
                        || lowerHost.endsWith("." + domain.toLowerCase(Locale.ROOT)));
                if (!matches) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
