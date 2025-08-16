package com.smartstudy.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity class representing a document in the SmartStudy system
 */
@Entity
@Table(name = "documents")
public class Document {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "filename", nullable = false)
    private String filename;
    
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "upload_date")
    private LocalDateTime uploadDate;
    
    @Column(name = "file_type")
    private String fileType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DocumentStatus status;
    
    @Column(name = "file_path")
    private String filePath;
    
    @Column(name = "content_summary", columnDefinition = "TEXT")
    private String contentSummary;
    
    // Constructors
    public Document() {
        this.uploadDate = LocalDateTime.now();
        this.status = DocumentStatus.PROCESSING;
    }
    
    public Document(String filename, String originalFilename, Long fileSize, String fileType) {
        this();
        this.filename = filename;
        this.originalFilename = originalFilename;
        this.fileSize = fileSize;
        this.fileType = fileType;
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
    
    public LocalDateTime getUploadDate() {
        return uploadDate;
    }
    
    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public DocumentStatus getStatus() {
        return status;
    }
    
    public void setStatus(DocumentStatus status) {
        this.status = status;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public String getContentSummary() {
        return contentSummary;
    }
    
    public void setContentSummary(String contentSummary) {
        this.contentSummary = contentSummary;
    }
    
    @Override
    public String toString() {
        return "Document{" +
                "id=" + id +
                ", filename='" + filename + '\'' +
                ", originalFilename='" + originalFilename + '\'' +
                ", fileSize=" + fileSize +
                ", uploadDate=" + uploadDate +
                ", fileType='" + fileType + '\'' +
                ", status=" + status +
                '}';
    }
    
    /**
     * Enum for document processing status
     */
    public enum DocumentStatus {
        PROCESSING,
        COMPLETED,
        FAILED
    }
}

