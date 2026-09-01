package com.sysdesign.autocomplete.service;

import com.sysdesign.autocomplete.model.SearchTerm;
import com.sysdesign.autocomplete.trie.Trie;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Reads the sample term-frequency dataset once when the application starts. */
@Component
public class SearchDatasetLoader {

    private final Trie trie;

    public SearchDatasetLoader(Trie trie) {
        this.trie = trie;
    }

    @PostConstruct
    public void loadSearchTermsIntoTrie() {
        ClassPathResource dataset = new ClassPathResource("search-terms.csv");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                dataset.getInputStream(), StandardCharsets.UTF_8))) {

            reader.readLine(); // Skip the CSV header: term,frequency

            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",", 2);
                String term = removeCsvQuotes(columns[0].trim()).toLowerCase();
                int frequency = Integer.parseInt(columns[1].trim());

                trie.insert(new SearchTerm(term, frequency));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load search-terms.csv", exception);
        }
    }

    private String removeCsvQuotes(String value) {
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            String valueWithoutOuterQuotes = value.substring(1, value.length() - 1);
            return valueWithoutOuterQuotes.replace("\"\"", "\"");
        }

        return value;
    }
}
