package com.sysdesign.autocomplete.controller;

import com.sysdesign.autocomplete.model.AutocompleteResponse;
import com.sysdesign.autocomplete.service.AutocompleteService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HTTP entry point for autocomplete requests. */
@RestController
@RequestMapping("/api/autocomplete")
@Validated
public class AutocompleteController {

    private final AutocompleteService autocompleteService;

    public AutocompleteController(AutocompleteService autocompleteService) {
        this.autocompleteService = autocompleteService;
    }

    @GetMapping
    public AutocompleteResponse getSuggestions(
            @RequestParam @NotBlank String prefix,
            @RequestParam(defaultValue = "5") @Min(1) @Max(10) int limit,
            @RequestParam(defaultValue = "true") boolean useCache) {
        return autocompleteService.getSuggestions(prefix, limit, useCache);
    }
}
