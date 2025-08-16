package com.smartstudy.repository;

import com.smartstudy.model.DocumentContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for DocumentContent entity operations
 */
@Repository
public interface DocumentContentRepository extends JpaRepository<DocumentContent, Long> {
    
    /**
     * Find all content for a specific document
     */
    List<DocumentContent> findByDocument_IdOrderByPageNumberAscSlideNumberAsc(Long documentId);
    
    /**
     * Find content by document ID and page number
     */
    List<DocumentContent> findByDocument_IdAndPageNumberOrderByPageNumber(Long documentId, Integer pageNumber);
    
    /**
     * Find content by document ID and slide number
     */
    List<DocumentContent> findByDocument_IdAndSlideNumberOrderBySlideNumber(Long documentId, Integer slideNumber);
    
    /**
     * Find content by topic
     */
    List<DocumentContent> findByTopicContainingIgnoreCase(String topic);
    
    /**
     * Find content by section title
     */
    List<DocumentContent> findBySectionTitleContainingIgnoreCase(String sectionTitle);
    
    /**
     * Find content with word count greater than specified value
     */
    List<DocumentContent> findByWordCountGreaterThan(Integer wordCount);
    
    /**
     * Find content by document ID and topic
     */
    List<DocumentContent> findByDocument_IdAndTopicContainingIgnoreCase(Long documentId, String topic);
    
    /**
     * Search content by text (case-insensitive)
     */
    @Query("SELECT dc FROM DocumentContent dc WHERE LOWER(dc.content) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    List<DocumentContent> findByContentContainingIgnoreCase(@Param("searchText") String searchText);
    
    /**
     * Search content by text within specific documents
     */
    @Query("SELECT dc FROM DocumentContent dc WHERE dc.document.id IN :documentIds AND LOWER(dc.content) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    List<DocumentContent> findByDocumentIdsAndContentContainingIgnoreCase(
            @Param("documentIds") List<Long> documentIds, 
            @Param("searchText") String searchText);
    
    /**
     * Get total word count for a document
     */
    @Query("SELECT SUM(dc.wordCount) FROM DocumentContent dc WHERE dc.document.id = :documentId")
    Integer getTotalWordCountByDocumentId(@Param("documentId") Long documentId);
    
    /**
     * Get unique topics for a document
     */
    @Query("SELECT DISTINCT dc.topic FROM DocumentContent dc WHERE dc.document.id = :documentId AND dc.topic IS NOT NULL")
    List<String> findUniqueTopicsByDocumentId(@Param("documentId") Long documentId);
    
    /**
     * Find content by content hash (for duplicate detection)
     */
    List<DocumentContent> findByContentHash(String contentHash);
    
    /**
     * Get content statistics for a document
     */
    @Query("SELECT COUNT(dc), SUM(dc.wordCount), AVG(dc.wordCount) FROM DocumentContent dc WHERE dc.document.id = :documentId")
    Object[] getContentStatisticsByDocumentId(@Param("documentId") Long documentId);
    
    /**
     * Find content with highest word count for a document
     */
    @Query("SELECT dc FROM DocumentContent dc WHERE dc.document.id = :documentId ORDER BY dc.wordCount DESC")
    List<DocumentContent> findTopContentByWordCount(@Param("documentId") Long documentId);
    
    /**
     * Delete all content for a specific document
     */
    void deleteByDocument_Id(Long documentId);
}
