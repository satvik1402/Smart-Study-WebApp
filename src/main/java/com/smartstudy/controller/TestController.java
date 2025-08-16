package com.smartstudy.controller;

import com.smartstudy.model.Document;
import com.smartstudy.model.DocumentContent;
import com.smartstudy.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Test controller for development and debugging
 */
@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {
    
    @Autowired
    private DocumentService documentService;
    
    /**
     * Test endpoint to check if the application is running
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("SmartStudy API is running! 🚀");
    }
    
    /**
     * Get all documents with their status
     */
    @GetMapping("/documents")
    public ResponseEntity<List<Document>> getAllDocuments() {
        try {
            List<Document> documents = documentService.getAllDocuments();
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get document processing status
     */
    @GetMapping("/documents/{id}/status")
    public ResponseEntity<Map<String, Object>> getDocumentStatus(@PathVariable Long id) {
        try {
            Document document = documentService.getDocumentById(id);
            if (document == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> status = Map.of(
                "id", document.getId(),
                "filename", document.getOriginalFilename(),
                "status", document.getStatus(),
                "uploadDate", document.getUploadDate(),
                "fileSize", document.getFileSize()
            );
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get document content for testing
     */
    @GetMapping("/documents/{id}/content")
    public ResponseEntity<List<DocumentContent>> getDocumentContent(@PathVariable Long id) {
        try {
            List<DocumentContent> content = documentService.getDocumentContent(id);
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get document statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = documentService.getDocumentStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Test search functionality
     */
    @GetMapping("/search")
    public ResponseEntity<String> testSearch() {
        return ResponseEntity.ok("Search functionality is available at /api/search");
    }
    
    /**
     * Test AI functionality
     */
    @GetMapping("/ai")
    public ResponseEntity<String> testAI() {
        return ResponseEntity.ok("AI functionality is available at /api/ai");
    }
}
