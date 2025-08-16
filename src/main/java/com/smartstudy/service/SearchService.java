package com.smartstudy.service;

import com.smartstudy.model.Document;
import com.smartstudy.model.DocumentContent;
import com.smartstudy.repository.DocumentContentRepository;
import com.smartstudy.repository.DocumentRepository;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for Lucene-based search functionality
 */
@Service
public class SearchService {
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private DocumentContentRepository documentContentRepository;
    
    @Value("${lucene.index.directory}")
    private String indexDirectoryPath;
    
    @Autowired
    private FSDirectory indexDirectory;
    
    @Autowired
    private StandardAnalyzer analyzer;
    
    @Autowired
    private IndexWriter indexWriter;
    
    /**
     * Index all document content
     */
    public void indexAllDocuments() throws IOException {
        System.out.println("🔄 Starting full reindex of all documents...");
        
        // Clear existing index
        indexWriter.deleteAll();
        
        // Get all documents with their content
        List<Document> documents = documentRepository.findAll();
        int indexedCount = 0;
        
        for (Document document : documents) {
            if (document.getStatus() == Document.DocumentStatus.COMPLETED) {
                List<DocumentContent> contents = documentContentRepository
                    .findByDocument_IdOrderByPageNumberAscSlideNumberAsc(document.getId());
                
                for (DocumentContent content : contents) {
                    indexDocumentContent(document, content);
                    indexedCount++;
                }
            }
        }
        
        // Commit changes
        indexWriter.commit();
        System.out.println("✅ Indexed " + indexedCount + " content blocks from " + documents.size() + " documents");
    }
    
    /**
     * Index a single document content
     */
    public void indexDocumentContent(Document document, DocumentContent content) throws IOException {
        org.apache.lucene.document.Document luceneDoc = new org.apache.lucene.document.Document();

        // Add fields to the Lucene document
        luceneDoc.add(new StringField("documentId", document.getId().toString(), Field.Store.YES));
        luceneDoc.add(new StringField("contentId", content.getId().toString(), Field.Store.YES));
        luceneDoc.add(new TextField("content", content.getContent(), Field.Store.YES));
        luceneDoc.add(new StringField("filename", document.getOriginalFilename(), Field.Store.YES));
        luceneDoc.add(new StringField("topic", content.getTopic() != null ? content.getTopic() : "", Field.Store.YES));
        luceneDoc.add(new StringField("sectionTitle", content.getSectionTitle() != null ? content.getSectionTitle() : "", Field.Store.YES));

        // Add page/slide information
        if (content.getPageNumber() != null) {
            luceneDoc.add(new IntPoint("pageNumber", content.getPageNumber()));
            luceneDoc.add(new StoredField("pageNumber", content.getPageNumber()));
        }
        if (content.getSlideNumber() != null) {
            luceneDoc.add(new IntPoint("slideNumber", content.getSlideNumber()));
            luceneDoc.add(new StoredField("slideNumber", content.getSlideNumber()));
        }

        // Add word count for relevance scoring
        if (content.getWordCount() != null) {
            luceneDoc.add(new IntPoint("wordCount", content.getWordCount()));
        }

        // Add timestamp
        luceneDoc.add(new LongPoint("timestamp", System.currentTimeMillis()));

        // Add to index
        indexWriter.addDocument(luceneDoc);
    }
    
    /**
     * Commit pending index changes
     */
    public void commitIndex() throws IOException {
        indexWriter.commit();
    }
    
    /**
     * Search for content using a query string
     */
    public List<SearchResult> search(String query, int maxResults) throws IOException, ParseException {
        try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            
            Query luceneQuery;
            if (query == null || query.trim().isEmpty()) {
                // If no query, get ALL documents ordered by document ID and page number
                luceneQuery = new MatchAllDocsQuery();
                // For empty queries, we want to get ALL results, not limit them
                maxResults = Integer.MAX_VALUE;
            } else {
                // Create a more flexible query that handles natural language better
                QueryParser parser = new QueryParser("content", analyzer);
                
                // Make the query more flexible by adding wildcards and handling common variations
                String processedQuery = query.trim();
                
                // If it's a single word or short phrase, make it more flexible
                if (processedQuery.split("\\s+").length <= 3) {
                    // Don't add wildcards if the query already contains special characters
                    if (!processedQuery.contains("*") && !processedQuery.contains("?") && !processedQuery.contains("(") && !processedQuery.contains(")")) {
                        // Add wildcards to make it more flexible, but only if it's not a single character
                        if (processedQuery.length() > 1) {
                            processedQuery = "*" + processedQuery + "*";
                        }
                    }
                }
                
                // Handle common variations (e.g., "dbms" vs "database management system")
                if (processedQuery.toLowerCase().contains("dbms")) {
                    processedQuery += " OR database OR database management system";
                }
                
                try {
                    luceneQuery = parser.parse(processedQuery);
                } catch (ParseException e) {
                    // If parsing fails, fall back to a simple term query
                    System.out.println("⚠️ Query parsing failed, using fallback: " + e.getMessage());
                    luceneQuery = new TermQuery(new Term("content", query.toLowerCase()));
                }
            }
            
            // Execute search with much higher max results to get ALL candidates
            TopDocs results = searcher.search(luceneQuery, Math.max(maxResults, 1000));
            
            // Convert results to our format
            List<SearchResult> searchResults = new ArrayList<>();
            for (ScoreDoc hit : results.scoreDocs) {
                org.apache.lucene.document.Document doc = searcher.doc(hit.doc);
                SearchResult result = new SearchResult();
                
                result.setDocumentId(Long.parseLong(doc.get("documentId")));
                result.setContentId(Long.parseLong(doc.get("contentId")));
                result.setContent(doc.get("content"));
                result.setFilename(doc.get("filename"));
                result.setTopic(doc.get("topic"));
                result.setSectionTitle(doc.get("sectionTitle"));
                result.setScore(hit.score);
                
                // Get page/slide information
                String pageNumber = doc.get("pageNumber");
                if (pageNumber != null) {
                    result.setPageNumber(Integer.parseInt(pageNumber));
                }
                
                String slideNumber = doc.get("slideNumber");
                if (slideNumber != null) {
                    result.setSlideNumber(Integer.parseInt(slideNumber));
                }
                
                searchResults.add(result);
            }
            
            // Sort by document ID first, then by page/slide number to maintain order
            searchResults.sort((a, b) -> {
                int docCompare = a.getDocumentId().compareTo(b.getDocumentId());
                if (docCompare != 0) return docCompare;
                
                // If same document, sort by page number, then slide number
                Integer aPage = a.getPageNumber() != null ? a.getPageNumber() : 0;
                Integer bPage = b.getPageNumber() != null ? b.getPageNumber() : 0;
                int pageCompare = aPage.compareTo(bPage);
                if (pageCompare != 0) return pageCompare;
                
                Integer aSlide = a.getSlideNumber() != null ? a.getSlideNumber() : 0;
                Integer bSlide = b.getSlideNumber() != null ? b.getSlideNumber() : 0;
                return aSlide.compareTo(bSlide);
            });
            
            // Return only the requested number of results (but for empty queries, return all)
            if (query == null || query.trim().isEmpty()) {
                return searchResults; // Return ALL results for empty queries
            } else {
                return searchResults.stream().limit(maxResults).collect(Collectors.toList());
            }
        }
    }
    
    /**
     * Search with filters
     */
    public List<SearchResult> searchWithFilters(String query, String filename, String topic, int maxResults) 
            throws IOException, ParseException {
        try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            
            // Create boolean query for combining filters
            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
            
            // Add main content query
            QueryParser parser = new QueryParser("content", analyzer);
            Query contentQuery = parser.parse(query);
            booleanQuery.add(contentQuery, BooleanClause.Occur.MUST);
            
            // Add filename filter if specified
            if (filename != null && !filename.trim().isEmpty()) {
                Query filenameQuery = new TermQuery(new Term("filename", filename));
                booleanQuery.add(filenameQuery, BooleanClause.Occur.FILTER);
            }
            
            // Add topic filter if specified
            if (topic != null && !topic.trim().isEmpty()) {
                Query topicQuery = new TermQuery(new Term("topic", topic));
                booleanQuery.add(topicQuery, BooleanClause.Occur.FILTER);
            }
            
            // Execute search
            TopDocs results = searcher.search(booleanQuery.build(), maxResults);
            
            // Convert results
            List<SearchResult> searchResults = new ArrayList<>();
            for (ScoreDoc hit : results.scoreDocs) {
                org.apache.lucene.document.Document doc = searcher.doc(hit.doc);
                SearchResult result = new SearchResult();
                
                result.setDocumentId(Long.parseLong(doc.get("documentId")));
                result.setContentId(Long.parseLong(doc.get("contentId")));
                result.setContent(doc.get("content"));
                result.setFilename(doc.get("filename"));
                result.setTopic(doc.get("topic"));
                result.setSectionTitle(doc.get("sectionTitle"));
                result.setScore(hit.score);
                
                String pageNumber = doc.get("pageNumber");
                if (pageNumber != null) {
                    result.setPageNumber(Integer.parseInt(pageNumber));
                }
                
                String slideNumber = doc.get("slideNumber");
                if (slideNumber != null) {
                    result.setSlideNumber(Integer.parseInt(slideNumber));
                }
                
                searchResults.add(result);
            }
            
            return searchResults;
        }
    }
    
    /**
     * Get search suggestions based on content
     */
    public List<String> getSearchSuggestions(String partialQuery, int maxSuggestions) throws IOException, ParseException {
        try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            
            // Search for content containing the partial query
            QueryParser parser = new QueryParser("content", analyzer);
            Query query = parser.parse(partialQuery + "*");
            
            TopDocs results = searcher.search(query, maxSuggestions);
            
            Set<String> suggestions = new HashSet<>();
            for (ScoreDoc hit : results.scoreDocs) {
                org.apache.lucene.document.Document doc = searcher.doc(hit.doc);
                String content = doc.get("content");
                
                // Extract potential suggestions from content
                String[] words = content.split("\\s+");
                for (String word : words) {
                    if (word.toLowerCase().startsWith(partialQuery.toLowerCase()) &&
                        word.length() > partialQuery.length()) {
                        suggestions.add(word.toLowerCase());
                        if (suggestions.size() >= maxSuggestions) break;
                    }
                }
            }
            
            return suggestions.stream().limit(maxSuggestions).collect(Collectors.toList());
        }
    }
    
    /**
     * Get search statistics
     */
    public Map<String, Object> getSearchStats() throws IOException {
        try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalDocuments", reader.numDocs());
            stats.put("indexSize", reader.maxDoc());
            stats.put("indexDirectory", indexDirectoryPath);
            return stats;
        }
    }
    
    /**
     * Reindex a specific document
     */
    public void reindexDocument(Long documentId) throws IOException {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }
        
        // Remove existing index entries for this document
        indexWriter.deleteDocuments(new Term("documentId", documentId.toString()));
        
        // Reindex all content for this document
        if (document.getStatus() == Document.DocumentStatus.COMPLETED) {
            List<DocumentContent> contents = documentContentRepository
                .findByDocument_IdOrderByPageNumberAscSlideNumberAsc(documentId);
            
            for (DocumentContent content : contents) {
                indexDocumentContent(document, content);
            }
        }
        
        indexWriter.commit();
        System.out.println("✅ Reindexed document: " + document.getOriginalFilename());
    }
    
    /**
     * Delete a document from the search index
     */
    public void deleteDocumentFromIndex(Long documentId) throws IOException {
        // Delete all entries for this document from the index
        indexWriter.deleteDocuments(new Term("documentId", documentId.toString()));
        indexWriter.commit();
        System.out.println("🗑️ Removed document " + documentId + " from search index");
    }
    
    /**
     * Search result class
     */
    public static class SearchResult {
        private Long documentId;
        private Long contentId;
        private String content;
        private String filename;
        private String topic;
        private String sectionTitle;
        private Integer pageNumber;
        private Integer slideNumber;
        private float score;
        
        // Getters and setters
        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }
        
        public Long getContentId() { return contentId; }
        public void setContentId(Long contentId) { this.contentId = contentId; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        
        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
        
        public String getSectionTitle() { return sectionTitle; }
        public void setSectionTitle(String sectionTitle) { this.sectionTitle = sectionTitle; }
        
        public Integer getPageNumber() { return pageNumber; }
        public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
        
        public Integer getSlideNumber() { return slideNumber; }
        public void setSlideNumber(Integer slideNumber) { this.slideNumber = slideNumber; }
        
        public float getScore() { return score; }
        public void setScore(float score) { this.score = score; }
    }
}
