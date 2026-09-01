package com.sysdesign.autocomplete.trie;

import com.sysdesign.autocomplete.model.SearchTerm;

import java.util.HashMap;
import java.util.Map;

/** A single character node in the Trie. */
class TrieNode {

    private final Map<Character, TrieNode> children = new HashMap<>();
    private boolean endOfWord;
    private SearchTerm searchTerm;

    Map<Character, TrieNode> getChildren() {
        return children;
    }

    boolean isEndOfWord() {
        return endOfWord;
    }

    void markAsEndOfWord(SearchTerm searchTerm) {
        this.endOfWord = true;
        this.searchTerm = searchTerm;
    }

    SearchTerm getSearchTerm() {
        return searchTerm;
    }
}
