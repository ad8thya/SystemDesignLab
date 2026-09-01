package com.sysdesign.autocomplete.model;

/** One searchable term and the number of times it has been searched. */
public record SearchTerm(String term, int frequency) {
}
