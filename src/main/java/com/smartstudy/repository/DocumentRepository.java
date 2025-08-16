package com.smartstudy.repository;

import com.smartstudy.model.Document;
import com.smartstudy.model.Document.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Document entity operations
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    /**
     * Find documents by status
     */
    List<Document> findByStatus(DocumentStatus status);
    
    /**
     * Find documents by file type
     */
    List<Document> findByFileType(String fileType);
    
    /**
     * Find documents uploaded after a specific date
     */
    List<Document> findByUploadDateAfter(LocalDateTime date);
    
    /**
     * Find documents by original filename (case-insensitive)
     */
    Optional<Document> findByOriginalFilenameIgnoreCase(String originalFilename);
    
    /**
     * Find documents with file size greater than specified value
     */
    List<Document> findByFileSizeGreaterThan(Long fileSize);
    
    /**
     * Find documents by status and file type
     */
    List<Document> findByStatusAndFileType(DocumentStatus status, String fileType);
    
    /**
     * Count documents by status
     */
    long countByStatus(DocumentStatus status);
    
    /**
     * Find documents with content summary containing the given text
     */
    @Query("SELECT d FROM Document d WHERE d.contentSummary LIKE %:text%")
    List<Document> findByContentSummaryContaining(@Param("text") String text);
    
    /**
     * Find documents uploaded in the last N days
     */
    @Query("SELECT d FROM Document d WHERE d.uploadDate >= :startDate")
    List<Document> findRecentDocuments(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Get total file size of all documents
     */
    @Query("SELECT SUM(d.fileSize) FROM Document d WHERE d.status = 'COMPLETED'")
    Long getTotalFileSize();
    
    /**
     * Find documents by multiple file types
     */
    @Query("SELECT d FROM Document d WHERE d.fileType IN :fileTypes")
    List<Document> findByFileTypes(@Param("fileTypes") List<String> fileTypes);
    
    /**
     * Find documents with processing errors (status = FAILED)
     */
    List<Document> findByStatusOrderByUploadDateDesc(DocumentStatus status);
}

