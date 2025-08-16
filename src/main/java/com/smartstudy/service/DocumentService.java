package com.smartstudy.service;

import com.smartstudy.model.Document;
import com.smartstudy.model.Document.DocumentStatus;
import com.smartstudy.model.DocumentContent;
import com.smartstudy.repository.DocumentRepository;
import com.smartstudy.repository.DocumentContentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service class for document management operations
 */
@Service
public class DocumentService {
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private DocumentProcessingService documentProcessingService;
    
    @Autowired
    private DocumentContentRepository documentContentRepository;
    
    @Autowired
    private SearchService searchService;
    
    @Value("${file.upload.directory}")
    private String uploadDirectory;
    
    @Value("${file.temp.directory}")
    private String tempDirectory;
    
    /**
     * Upload a document file
     */
    public Document uploadDocument(MultipartFile file) throws IOException {
        System.out.println("📤 DocumentService.uploadDocument called for: " + file.getOriginalFilename());
        
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid filename");
        }
        
        // Check file size (max 100MB)
        if (file.getSize() > 100 * 1024 * 1024) {
            throw new IllegalArgumentException("File size too large. Maximum allowed: 100MB");
        }
        
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            System.out.println("📁 Created upload directory: " + uploadPath);
        }
        
        // Generate unique filename
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = System.currentTimeMillis() + "_" + originalFilename;
        
        System.out.println("📝 Generated filename: " + uniqueFilename);
        System.out.println("📁 File extension: " + fileExtension);
        
        // Save file to disk
        Path filePath = uploadPath.resolve(uniqueFilename);
        try {
            Files.copy(file.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.println("💾 File saved to: " + filePath);
        } catch (Exception e) {
            System.err.println("❌ Failed to save file: " + e.getMessage());
            throw new IOException("Failed to save uploaded file: " + e.getMessage(), e);
        }
        
        // Create document entity
        Document document = new Document();
        document.setFilename(uniqueFilename);
        document.setOriginalFilename(originalFilename);
        document.setFileSize(file.getSize());
        document.setFileType(fileExtension);
        document.setFilePath(filePath.toString());
        document.setStatus(DocumentStatus.PROCESSING); // Start with PROCESSING
        document.setUploadDate(LocalDateTime.now());
        
        // Save to database
        Document savedDocument = documentRepository.save(document);
        System.out.println("💾 Document saved to database with ID: " + savedDocument.getId());
        
        // Start async processing for document content extraction
        try {
            documentProcessingService.processDocumentAsync(savedDocument);
            System.out.println("🔄 Started async processing for document: " + savedDocument.getId());
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Failed to start async processing: " + e.getMessage());
            // Don't fail the upload if processing fails
        }
        
        return savedDocument;
    }
    
    /**
     * Get all documents
     */
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }
    
    /**
     * Get document by ID
     */
    public Document getDocumentById(Long id) {
        return documentRepository.findById(id).orElse(null);
    }
    
    /**
     * Get documents by status
     */
    public List<Document> getDocumentsByStatus(String status) {
        try {
            DocumentStatus documentStatus = DocumentStatus.valueOf(status.toUpperCase());
            return documentRepository.findByStatus(documentStatus);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
    }
    
    /**
     * Delete document by ID
     */
    @Transactional
    public boolean deleteDocument(Long id) {
        Document document = documentRepository.findById(id).orElse(null);
        if (document != null) {
            System.out.println("🗑️ Deleting document: " + document.getOriginalFilename() + " (ID: " + id + ")");
            
            // Delete file from disk
            try {
                Path filePath = Paths.get(document.getFilePath());
                if (Files.exists(filePath)) {
                    Files.deleteIfExists(filePath);
                    System.out.println("✅ Deleted file from disk: " + filePath);
                } else {
                    System.out.println("⚠️ File not found on disk: " + filePath);
                }
            } catch (IOException e) {
                // Log error but continue with database deletion
                System.err.println("❌ Error deleting file: " + e.getMessage());
            }
            
            // Delete from search index
            try {
                searchService.deleteDocumentFromIndex(id);
                System.out.println("✅ Removed from search index");
            } catch (Exception e) {
                System.err.println("⚠️ Warning: Failed to remove from search index: " + e.getMessage());
            }
            
            // Delete content first (to avoid foreign key constraint)
            try {
                documentContentRepository.deleteByDocument_Id(id);
                System.out.println("✅ Deleted content from database");
            } catch (Exception e) {
                System.err.println("⚠️ Warning: Could not delete content: " + e.getMessage());
            }
            
            // Delete from database
            documentRepository.delete(document);
            System.out.println("✅ Deleted from database");
            return true;
        }
        return false;
    }
    
    /**
     * Delete all documents
     */
    @Transactional
    public int deleteAllDocuments() {
        System.out.println("🗑️ Deleting all documents...");
        
        List<Document> allDocuments = documentRepository.findAll();
        int deletedCount = 0;
        
        for (Document document : allDocuments) {
            try {
                // Delete file from disk
                try {
                    Path filePath = Paths.get(document.getFilePath());
                    if (Files.exists(filePath)) {
                        Files.deleteIfExists(filePath);
                        System.out.println("✅ Deleted file: " + document.getOriginalFilename());
                    }
                } catch (IOException e) {
                    System.err.println("⚠️ Could not delete file: " + document.getOriginalFilename());
                }
                
                // Delete from search index
                try {
                    searchService.deleteDocumentFromIndex(document.getId());
                } catch (Exception e) {
                    System.err.println("⚠️ Could not remove from search index: " + document.getId());
                }
                
                deletedCount++;
            } catch (Exception e) {
                System.err.println("❌ Error deleting document " + document.getId() + ": " + e.getMessage());
            }
        }
        
        // Delete all content and documents from database
        try {
            documentContentRepository.deleteAll();
            documentRepository.deleteAll();
            System.out.println("✅ Deleted all content and documents from database");
        } catch (Exception e) {
            System.err.println("❌ Error deleting from database: " + e.getMessage());
        }
        
        System.out.println("🗑️ Successfully deleted " + deletedCount + " documents");
        return deletedCount;
    }
    
    /**
     * Get document statistics
     */
    public Map<String, Object> getDocumentStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Total documents
        long totalDocuments = documentRepository.count();
        stats.put("totalDocuments", totalDocuments);
        
        // Documents by status
        long processingCount = documentRepository.countByStatus(DocumentStatus.PROCESSING);
        long completedCount = documentRepository.countByStatus(DocumentStatus.COMPLETED);
        long failedCount = documentRepository.countByStatus(DocumentStatus.FAILED);
        
        stats.put("processingCount", processingCount);
        stats.put("completedCount", completedCount);
        stats.put("failedCount", failedCount);
        
        // Total file size
        Long totalFileSize = documentRepository.getTotalFileSize();
        stats.put("totalFileSize", totalFileSize != null ? totalFileSize : 0);
        
        // Recent documents (last 7 days)
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<Document> recentDocuments = documentRepository.findRecentDocuments(weekAgo);
        stats.put("recentDocumentsCount", recentDocuments.size());
        
        return stats;
    }
    
    /**
     * Update document status
     */
    public Document updateDocumentStatus(Long id, DocumentStatus status) {
        Document document = documentRepository.findById(id).orElse(null);
        if (document != null) {
            document.setStatus(status);
            return documentRepository.save(document);
        }
        return null;
    }
    
    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
    
    /**
     * Get document content by document ID
     */
    public List<DocumentContent> getDocumentContent(Long documentId) {
        return documentContentRepository.findByDocument_IdOrderByPageNumberAscSlideNumberAsc(documentId);
    }
    
    /**
     * Validate file type
     */
    public boolean isValidFileType(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        return extension.equals(".zip") || 
               extension.equals(".pdf") || 
               extension.equals(".doc") || 
               extension.equals(".docx") || 
               extension.equals(".ppt") || 
               extension.equals(".pptx");
    }
}
