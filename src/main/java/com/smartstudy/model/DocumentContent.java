package com.smartstudy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity class representing extracted content from documents
 */
@Entity
@Table(name = "document_content")
public class DocumentContent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    @JsonIgnore
    private Document document;
    
    @Column(name = "page_number")
    private Integer pageNumber;
    
    @Column(name = "slide_number")
    private Integer slideNumber;
    
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(name = "topic")
    private String topic;
    
    @Column(name = "section_title")
    private String sectionTitle;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "content_hash")
    private String contentHash;
    
    @Column(name = "word_count")
    private Integer wordCount;
    
    // Constructors
    public DocumentContent() {
        this.createdAt = LocalDateTime.now();
    }
    
    public DocumentContent(Document document, String content, Integer pageNumber, Integer slideNumber) {
        this();
        this.document = document;
        this.content = content;
        this.pageNumber = pageNumber;
        this.slideNumber = slideNumber;
        this.wordCount = content != null ? content.split("\\s+").length : 0;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Document getDocument() {
        return document;
    }
    
    public void setDocument(Document document) {
        this.document = document;
    }
    
    public Integer getPageNumber() {
        return pageNumber;
    }
    
    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }
    
    public Integer getSlideNumber() {
        return slideNumber;
    }
    
    public void setSlideNumber(Integer slideNumber) {
        this.slideNumber = slideNumber;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
        this.wordCount = content != null ? content.split("\\s+").length : 0;
    }
    
    public String getTopic() {
        return topic;
    }
    
    public void setTopic(String topic) {
        this.topic = topic;
    }
    
    public String getSectionTitle() {
        return sectionTitle;
    }
    
    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle = sectionTitle;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getContentHash() {
        return contentHash;
    }
    
    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }
    
    public Integer getWordCount() {
        return wordCount;
    }
    
    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }
    
    /**
     * Get the document ID without triggering lazy loading
     */
    public Long getDocumentId() {
        return document != null ? document.getId() : null;
    }
    
    /**
     * Get the location identifier (page or slide number)
     */
    public String getLocationIdentifier() {
        if (pageNumber != null) {
            return "Page " + pageNumber;
        } else if (slideNumber != null) {
            return "Slide " + slideNumber;
        }
        return "Unknown";
    }
    
    @Override
    public String toString() {
        return "DocumentContent{" +
                "id=" + id +
                ", documentId=" + (document != null ? document.getId() : null) +
                ", pageNumber=" + pageNumber +
                ", slideNumber=" + slideNumber +
                ", topic='" + topic + '\'' +
                ", wordCount=" + wordCount +
                '}';
    }
}
