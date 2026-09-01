package com.sysdesign.autocomplete.model;

import java.util.List;

/** Complete response for one autocomplete request. */
public record AutocompleteResponse(
        String prefix,
        int limit,
        boolean cacheHit,
        List<SearchSuggestion> suggestions) {
}
