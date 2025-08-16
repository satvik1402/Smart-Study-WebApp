package com.smartstudy.dto;

import java.util.List;

/**
 * DTO for search request
 */
public class SearchRequest {
    
    private String query;
    private List<Long> documentIds;
    private Boolean includeSummarization;
    private Integer maxResults;
    private String searchType; // "EXACT", "FUZZY", "SEMANTIC"
    
    // Constructors
    public SearchRequest() {}
    
    public SearchRequest(String query) {
        this.query = query;
        this.maxResults = 10;
        this.includeSummarization = false;
        this.searchType = "EXACT";
    }
    
    // Getters and Setters
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    public List<Long> getDocumentIds() {
        return documentIds;
    }
    
    public void setDocumentIds(List<Long> documentIds) {
        this.documentIds = documentIds;
    }
    
    public Boolean getIncludeSummarization() {
        return includeSummarization;
    }
    
    public void setIncludeSummarization(Boolean includeSummarization) {
        this.includeSummarization = includeSummarization;
    }
    
    public Integer getMaxResults() {
        return maxResults;
    }
    
    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }
    
    public String getSearchType() {
        return searchType;
    }
    
    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }
}

