package com.smartstudy.dto;

import com.smartstudy.model.Document;
import java.time.LocalDateTime;

/**
 * DTO for document upload response
 */
public class DocumentUploadResponse {
    
    private Long id;
    private String filename;
    private String originalFilename;
    private Long fileSize;
    private String fileType;
    private String status;
    private LocalDateTime uploadDate;
    private String message;
    private boolean success;
    private Document document;
    
    // Constructors
    public DocumentUploadResponse() {}
    
    public DocumentUploadResponse(Document document, String message) {
        this.id = document.getId();
        this.filename = document.getFilename();
        this.originalFilename = document.getOriginalFilename();
        this.fileSize = document.getFileSize();
        this.fileType = document.getFileType();
        this.status = document.getStatus().name();
        this.uploadDate = document.getUploadDate();
        this.message = message;
        this.success = true;
        this.document = document;
    }
    
    public DocumentUploadResponse(String errorMessage) {
        this.message = errorMessage;
        this.success = false;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public String getOriginalFilename() {
        return originalFilename;
    }
    
    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getUploadDate() {
        return uploadDate;
    }
    
    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public Document getDocument() {
        return document;
    }
    
    public void setDocument(Document document) {
        this.document = document;
    }
}
