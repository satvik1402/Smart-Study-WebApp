package com.smartstudy.controller;

import com.smartstudy.dto.DocumentUploadResponse;
import com.smartstudy.model.Document;
import com.smartstudy.model.DocumentContent;
import com.smartstudy.service.DocumentService;
import com.smartstudy.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * REST controller for document management operations
 */
@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {
    
    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private AnalyticsService analyticsService;
    
    /**
     * Upload a ZIP file containing study materials
     */
    @PostMapping("/upload")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file) {
        
        try {
            System.out.println("📤 DocumentController.uploadDocument called");
            System.out.println("📄 File: " + file.getOriginalFilename() + " (" + file.getSize() + " bytes)");
            
            // Validate file
            if (file == null) {
                return ResponseEntity.badRequest()
                    .body(new DocumentUploadResponse("No file provided"));
            }
            
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new DocumentUploadResponse("File is empty"));
            }
            
            // Check file size (max 100MB)
            if (file.getSize() > 100 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                    .body(new DocumentUploadResponse("File size too large. Maximum allowed: 100MB"));
            }
            
            // Upload and process document
            Document document = documentService.uploadDocument(file);
            
            DocumentUploadResponse response = new DocumentUploadResponse(
                document, 
                "Document uploaded successfully and processing started"
            );
            
            System.out.println("✅ Document uploaded successfully: " + document.getId());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Validation error: " + e.getMessage());
            return ResponseEntity.badRequest()
                .body(new DocumentUploadResponse("Validation error: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Upload error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new DocumentUploadResponse("Error uploading document: " + e.getMessage()));
        }
    }
    
    /**
     * View file inline by document ID
     */
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> viewDocumentFile(@PathVariable Long id) {
        try {
            Document document = documentService.getDocumentById(id);
            if (document == null) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = Paths.get(document.getFilePath());
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            String contentType = Files.probeContentType(filePath);
            if (contentType == null || MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(contentType)) {
                // Fallback by extension for better inline rendering
                String filenameLower = document.getOriginalFilename() != null ? document.getOriginalFilename().toLowerCase() : filePath.getFileName().toString().toLowerCase();
                if (filenameLower.endsWith(".pdf")) {
                    contentType = MediaType.APPLICATION_PDF_VALUE;
                } else if (filenameLower.endsWith(".png")) {
                    contentType = MediaType.IMAGE_PNG_VALUE;
                } else if (filenameLower.endsWith(".jpg") || filenameLower.endsWith(".jpeg")) {
                    contentType = MediaType.IMAGE_JPEG_VALUE;
                } else if (filenameLower.endsWith(".gif")) {
                    contentType = MediaType.IMAGE_GIF_VALUE;
                } else {
                    contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
                }
            }
            long contentLength = Files.size(filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + document.getOriginalFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get all documents, optionally filtered by status and limited
     */
    @GetMapping
    public ResponseEntity<List<Document>> getAllDocuments(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "limit", required = false) Integer limit) {
        try {
            List<Document> documents;
            if (status != null && !status.isBlank()) {
                documents = documentService.getDocumentsByStatus(status);
            } else {
                documents = documentService.getAllDocuments();
            }
            if (limit != null && limit > 0 && documents.size() > limit) {
                documents = documents.subList(0, limit);
            }
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get document by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocumentById(@PathVariable Long id) {
        try {
            Document document = documentService.getDocumentById(id);
            if (document != null) {
                return ResponseEntity.ok(document);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get documents by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Document>> getDocumentsByStatus(
            @PathVariable String status) {
        try {
            List<Document> documents = documentService.getDocumentsByStatus(status);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
        /**
     * Delete document by ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDocument(@PathVariable Long id) {
        try {
            boolean deleted = documentService.deleteDocument(id);
            if (deleted) {
                return ResponseEntity.ok("Document deleted successfully");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error deleting document: " + e.getMessage());
        }
    }
    
    /**
     * Delete all documents
     */
    @DeleteMapping("/all")
    public ResponseEntity<String> deleteAllDocuments() {
        try {
            int deletedCount = documentService.deleteAllDocuments();
            return ResponseEntity.ok("Successfully deleted " + deletedCount + " documents");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error deleting documents: " + e.getMessage());
        }
    }
    
    /**
     * Get document statistics / analytics overview
     */
    @GetMapping("/stats")
    public ResponseEntity<Object> getDocumentStats(
            @RequestParam(value = "activityPeriod", defaultValue = "7") int activityPeriod,
            @RequestParam(value = "metricsPeriod", defaultValue = "7") int metricsPeriod) {
        try {
            Object stats = analyticsService.getOverview(activityPeriod, metricsPeriod);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            // Fallback to document-only stats so UI doesn't break
            try {
                Object fallback = documentService.getDocumentStats();
                return ResponseEntity.ok(fallback);
            } catch (Exception inner) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
    }
    
    /**
     * Get document content by document ID
     */
    @GetMapping("/{id}/content")
    public ResponseEntity<List<DocumentContent>> getDocumentContent(@PathVariable Long id) {
        try {
            Document document = documentService.getDocumentById(id);
            if (document == null) {
                return ResponseEntity.notFound().build();
            }
            
            List<DocumentContent> content = documentService.getDocumentContent(id);
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Document service is running");
    }
}
