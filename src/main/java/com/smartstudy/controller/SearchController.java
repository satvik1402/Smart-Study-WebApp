package com.smartstudy.controller;

import com.smartstudy.service.SearchService;
import com.smartstudy.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for search functionality
 */
@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "*")
public class SearchController {
    
    @Autowired
    private SearchService searchService;
    
    @Autowired
    private AnalyticsService analyticsService;
    
    /**
     * Basic search endpoint
     */
    @GetMapping
    public ResponseEntity<List<SearchService.SearchResult>> search(
            @RequestParam("q") String query,
            @RequestParam(value = "maxResults", defaultValue = "20") int maxResults) {
        
        try {
            try { analyticsService.incrementSearchCount(); } catch (Exception ignore) {}
            List<SearchService.SearchResult> results = searchService.search(query, maxResults);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Advanced search with filters
     */
    @GetMapping("/advanced")
    public ResponseEntity<List<SearchService.SearchResult>> advancedSearch(
            @RequestParam("q") String query,
            @RequestParam(value = "filename", required = false) String filename,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "maxResults", defaultValue = "20") int maxResults) {
        
        try {
            try { analyticsService.incrementSearchCount(); } catch (Exception ignore) {}
            List<SearchService.SearchResult> results = searchService.searchWithFilters(query, filename, topic, maxResults);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get search suggestions
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSuggestions(
            @RequestParam("q") String partialQuery,
            @RequestParam(value = "maxSuggestions", defaultValue = "10") int maxSuggestions) {
        
        try {
            try { analyticsService.incrementSearchCount(); } catch (Exception ignore) {}
            List<String> suggestions = searchService.getSearchSuggestions(partialQuery, maxSuggestions);
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get search statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSearchStats() {
        try {
            Map<String, Object> stats = searchService.getSearchStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Reindex all documents
     */
    @PostMapping("/reindex")
    public ResponseEntity<String> reindexAll() {
        try {
            searchService.indexAllDocuments();
            return ResponseEntity.ok("All documents reindexed successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error reindexing documents: " + e.getMessage());
        }
    }
    
    /**
     * Reindex a specific document
     */
    @PostMapping("/reindex/{documentId}")
    public ResponseEntity<String> reindexDocument(@PathVariable Long documentId) {
        try {
            searchService.reindexDocument(documentId);
            return ResponseEntity.ok("Document reindexed successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error reindexing document: " + e.getMessage());
        }
    }
    
    /**
     * Health check for search service
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Search service is running");
    }
}
