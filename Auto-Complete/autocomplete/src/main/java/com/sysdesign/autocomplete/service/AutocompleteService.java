package com.sysdesign.autocomplete.service;

import com.sysdesign.autocomplete.model.AutocompleteResponse;
import com.sysdesign.autocomplete.model.SearchSuggestion;
import com.sysdesign.autocomplete.model.SearchTerm;
import com.sysdesign.autocomplete.trie.Trie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

/** Coordinates cache lookup, Trie lookup, sorting, and cache storage. */
@Service
public class AutocompleteService {

    // Change this version whenever the vocabulary dataset changes.
    // It prevents cached results from an older dataset being reused.
    private static final String CACHE_KEY_PREFIX = "autocomplete:aol-v2:";

    private final Trie trie;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration cacheTimeToLive;

    public AutocompleteService(
            Trie trie,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${autocomplete.cache.ttl-seconds:300}") long cacheTtlSeconds) {
        this.trie = trie;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheTimeToLive = Duration.ofSeconds(cacheTtlSeconds);
    }

    public AutocompleteResponse getSuggestions(String requestedPrefix, int limit, boolean useCache) {
        String prefix = requestedPrefix.trim().toLowerCase();
        String cacheKey = buildCacheKey(prefix, limit);

        if (useCache) {
            List<SearchSuggestion> cachedSuggestions = getSuggestionsFromCache(cacheKey);
            if (cachedSuggestions != null) {
                return new AutocompleteResponse(prefix, limit, true, cachedSuggestions);
            }
        }

        List<SearchSuggestion> suggestions = findSuggestionsInTrie(prefix, limit);
        if (useCache) {
            saveSuggestionsToCache(cacheKey, suggestions);
        }

        return new AutocompleteResponse(prefix, limit, false, suggestions);
    }

    private List<SearchSuggestion> getSuggestionsFromCache(String cacheKey) {
        String cachedJson = redisTemplate.opsForValue().get(cacheKey);

        if (cachedJson == null) {
            return null;
        }

        try {
            return objectMapper.readValue(cachedJson, new TypeReference<List<SearchSuggestion>>() { });
        } catch (Exception exception) {
            redisTemplate.delete(cacheKey);
            return null;
        }
    }

    private List<SearchSuggestion> findSuggestionsInTrie(String prefix, int limit) {
        List<SearchTerm> matchingTerms = trie.findTermsStartingWith(prefix);

        return matchingTerms.stream()
                .sorted(Comparator.comparingInt(SearchTerm::frequency)
                        .reversed()
                        .thenComparing(SearchTerm::term))
                .limit(limit)
                .map(term -> new SearchSuggestion(term.term(), term.frequency()))
                .toList();
    }

    private void saveSuggestionsToCache(String cacheKey, List<SearchSuggestion> suggestions) {
        try {
            String suggestionsAsJson = objectMapper.writeValueAsString(suggestions);
            redisTemplate.opsForValue().set(cacheKey, suggestionsAsJson, cacheTimeToLive);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not save suggestions in Redis", exception);
        }
    }

    private String buildCacheKey(String prefix, int limit) {
        return CACHE_KEY_PREFIX + prefix + ":limit:" + limit;
    }
}
