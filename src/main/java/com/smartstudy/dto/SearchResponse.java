package com.smartstudy.dto;

import java.util.List;

/**
 * DTO for search response
 */
public class SearchResponse {
    
    private String query;
    private List<SearchResult> results;
    private String summary;
    private Integer totalResults;
    private Long searchTimeMs;
    private List<String> sources;
    
    // Constructors
    public SearchResponse() {}
    
    public SearchResponse(String query, List<SearchResult> results) {
        this.query = query;
        this.results = results;
        this.totalResults = results != null ? results.size() : 0;
    }
    
    // Getters and Setters
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public List<SearchResult> getResults() {
        return results;
    }
    
    public void setResults(List<SearchResult> results) {
        this.results = results;
        this.totalResults = results != null ? results.size() : 0;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public Integer getTotalResults() {
        return totalResults;
    }
    
    public void setTotalResults(Integer totalResults) {
        this.totalResults = totalResults;
    }
    
    public Long getSearchTimeMs() {
        return searchTimeMs;
    }
    
    public void setSearchTimeMs(Long searchTimeMs) {
        this.searchTimeMs = searchTimeMs;
    }
    
    public List<String> getSources() {
        return sources;
    }
    
    public void setSources(List<String> sources) {
        this.sources = sources;
    }
    
    /**
     * Inner class for individual search results
     */
    public static class SearchResult {
        private String content;
        private String sourceDocument;
        private String location; // Page/Slide number
        private String topic;
        private Double relevanceScore;
        private String sectionTitle;
        
        // Constructors
        public SearchResult() {}
        
        public SearchResult(String content, String sourceDocument, String location) {
            this.content = content;
            this.sourceDocument = sourceDocument;
            this.location = location;
        }
        
        // Getters and Setters
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public String getSourceDocument() {
            return sourceDocument;
        }
        
        public void setSourceDocument(String sourceDocument) {
            this.sourceDocument = sourceDocument;
        }
        
        public String getLocation() {
            return location;
        }
        
        public void setLocation(String location) {
            this.location = location;
        }
        
        public String getTopic() {
            return topic;
        }
        
        public void setTopic(String topic) {
            this.topic = topic;
        }
        
        public Double getRelevanceScore() {
            return relevanceScore;
        }
        
        public void setRelevanceScore(Double relevanceScore) {
            this.relevanceScore = relevanceScore;
        }
        
        public String getSectionTitle() {
            return sectionTitle;
        }
        
        public void setSectionTitle(String sectionTitle) {
            this.sectionTitle = sectionTitle;
        }
    }
}

