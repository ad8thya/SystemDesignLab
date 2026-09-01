package com.sysdesign.autocomplete.model;

/** A suggestion returned to an API caller. */
public record SearchSuggestion(String term, int frequency) {
}
