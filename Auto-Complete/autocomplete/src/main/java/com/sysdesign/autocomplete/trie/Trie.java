package com.sysdesign.autocomplete.trie;

import com.sysdesign.autocomplete.model.SearchTerm;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Stores terms character by character and retrieves every term under a prefix. */
@Component
public class Trie {

    private final TrieNode root = new TrieNode();

    public void insert(SearchTerm searchTerm) {
        TrieNode currentNode = root;

        for (char character : searchTerm.term().toCharArray()) {
            TrieNode nextNode = currentNode.getChildren().get(character);

            if (nextNode == null) {
                nextNode = new TrieNode();
                currentNode.getChildren().put(character, nextNode);
            }

            currentNode = nextNode;
        }

        currentNode.markAsEndOfWord(searchTerm);
    }

    public List<SearchTerm> findTermsStartingWith(String prefix) {
        TrieNode prefixNode = findNodeForPrefix(prefix);
        List<SearchTerm> matchingTerms = new ArrayList<>();

        if (prefixNode != null) {
            collectTermsBelowNode(prefixNode, matchingTerms);
        }

        return matchingTerms;
    }

    private TrieNode findNodeForPrefix(String prefix) {
        TrieNode currentNode = root;

        for (char character : prefix.toCharArray()) {
            currentNode = currentNode.getChildren().get(character);

            if (currentNode == null) {
                return null;
            }
        }

        return currentNode;
    }

    private void collectTermsBelowNode(TrieNode node, List<SearchTerm> matchingTerms) {
        if (node.isEndOfWord()) {
            matchingTerms.add(node.getSearchTerm());
        }

        for (TrieNode childNode : node.getChildren().values()) {
            collectTermsBelowNode(childNode, matchingTerms);
        }
    }
}
