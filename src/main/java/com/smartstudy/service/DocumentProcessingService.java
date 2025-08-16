package com.smartstudy.service;

import com.smartstudy.model.Document;
import com.smartstudy.model.DocumentContent;
import com.smartstudy.model.Document.DocumentStatus;
import com.smartstudy.repository.DocumentContentRepository;
import com.smartstudy.repository.DocumentRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Service for processing uploaded documents and extracting content
 */
@Service
public class DocumentProcessingService {
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private DocumentContentRepository documentContentRepository;
    
    @Autowired
    private SearchService searchService;
    
    private final Tika tika = new Tika();
    
    /**
     * Process a document asynchronously
     */
    @Async
    public void processDocumentAsync(Document document) {
        try {
            System.out.println("🔄 Starting processing for document: " + document.getOriginalFilename());
            
            // Update status to processing
            updateDocumentStatus(document.getId(), DocumentStatus.PROCESSING);
            
            List<DocumentContent> extractedContent = new ArrayList<>();
            
            if (document.getFileType().equalsIgnoreCase(".zip")) {
                // Process ZIP file
                extractedContent = processZipFile(document);
            } else {
                // Process single file
                extractedContent = processSingleFile(document);
            }
            
            // Save extracted content to database
            if (!extractedContent.isEmpty()) {
                documentContentRepository.saveAll(extractedContent);
                
                // Index the content for search
                try {
                    for (DocumentContent content : extractedContent) {
                        searchService.indexDocumentContent(document, content);
                    }
                    searchService.commitIndex();
                    System.out.println("🔍 Indexed " + extractedContent.size() + " content blocks for search");
                    
                    // Force a full reindex to ensure all content is properly indexed
                    System.out.println("🔄 Forcing full reindex to ensure all content is available...");
                    searchService.indexAllDocuments();
                    System.out.println("✅ Full reindex completed");
                    
                } catch (Exception e) {
                    System.err.println("⚠️ Warning: Failed to index content for search: " + e.getMessage());
                }
                
                // Update document status to completed
                updateDocumentStatus(document.getId(), DocumentStatus.COMPLETED);
                
                System.out.println("✅ Successfully processed document: " + document.getOriginalFilename() + 
                                 " (Extracted " + extractedContent.size() + " content blocks)");
            } else {
                throw new RuntimeException("No content could be extracted from the document");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error processing document: " + document.getOriginalFilename() + " - " + e.getMessage());
            updateDocumentStatus(document.getId(), DocumentStatus.FAILED);
        }
    }
    
    /**
     * Process a ZIP file containing multiple documents
     */
    private List<DocumentContent> processZipFile(Document document) throws IOException {
        List<DocumentContent> allContent = new ArrayList<>();
        Path zipPath = Paths.get(document.getFilePath());
        
        System.out.println("📦 Processing ZIP file: " + document.getOriginalFilename());
        System.out.println("📁 ZIP path: " + zipPath);
        
        if (!Files.exists(zipPath)) {
            throw new IOException("ZIP file not found at path: " + zipPath);
        }
        
        // Get ZIP file size for progress tracking
        long zipFileSize = Files.size(zipPath);
        System.out.println("📊 ZIP file size: " + formatFileSize(zipFileSize));
        
        // Check if ZIP file is too large (more than 50MB)
        if (zipFileSize > 50 * 1024 * 1024) {
            System.err.println("⚠️ Warning: Large ZIP file detected (" + formatFileSize(zipFileSize) + "). Processing may take a long time.");
        }
        
        // Add timeout protection for large files
        long maxProcessingTime = 30 * 60 * 1000; // 30 minutes max
        long startTime = System.currentTimeMillis();
        
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            int processedFiles = 0;
            int totalFiles = 0;
            int maxFilesToProcess = 100; // Limit to prevent infinite processing
            
            // First pass: count total files (with limit)
            List<ZipEntry> entries = new ArrayList<>();
            while ((entry = zipInputStream.getNextEntry()) != null && totalFiles < maxFilesToProcess) {
                if (!entry.isDirectory() && isSupportedFileType(entry.getName())) {
                    entries.add(entry);
                }
                totalFiles++;
                zipInputStream.closeEntry();
            }
            
            System.out.println("🔍 Found " + totalFiles + " total entries, processing " + entries.size() + " supported files");
            System.out.println("⏱️ Starting processing with 30-minute timeout...");
            
            // Second pass: process files
            try (ZipInputStream processZipStream = new ZipInputStream(Files.newInputStream(zipPath))) {
                for (int i = 0; i < entries.size(); i++) {
                    // Check timeout
                    if (System.currentTimeMillis() - startTime > maxProcessingTime) {
                        System.err.println("⏰ Processing timeout reached (30 minutes). Stopping.");
                        break;
                    }
                    
                    entry = processZipStream.getNextEntry();
                    if (entry == null) break;
                    
                    String entryName = entry.getName();
                    long entrySize = entry.getSize();
                    
                    System.out.println("  📄 [" + (i + 1) + "/" + entries.size() + "] Processing: " + entryName);
                    System.out.println("     📊 Entry size: " + formatFileSize(entrySize));
                    
                    if (!entry.isDirectory() && isSupportedFileType(entryName)) {
                        try {
                            // Extract file to temporary location
                            Path tempFile = extractZipEntry(processZipStream, entryName);
                            
                            try {
                                // Process the extracted file with timeout
                                List<DocumentContent> fileContent = extractContentFromFile(tempFile, entryName, document);
                                allContent.addAll(fileContent);
                                processedFiles++;
                                
                                // Calculate progress
                                double progressPercent = (double) (i + 1) / entries.size() * 100;
                                long elapsedTime = System.currentTimeMillis() - startTime;
                                long remainingTime = maxProcessingTime - elapsedTime;
                                
                                System.out.println("    ✅ Successfully processed: " + entryName);
                                System.out.println("       📊 Extracted " + fileContent.size() + " content blocks");
                                System.out.println("       📈 Progress: " + String.format("%.1f", progressPercent) + "%");
                                System.out.println("       ⏱️ Elapsed: " + formatDuration(elapsedTime) + " | Remaining: " + formatDuration(remainingTime));
                                
                            } catch (Exception e) {
                                System.err.println("    ❌ Error processing file " + entryName + ": " + e.getMessage());
                                // Continue with other files instead of failing completely
                            } finally {
                                // Clean up temporary file
                                try {
                                    Files.deleteIfExists(tempFile);
                                } catch (Exception e) {
                                    System.err.println("    ⚠️ Warning: Could not delete temp file: " + e.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("    ❌ Error extracting file " + entryName + ": " + e.getMessage());
                            // Continue with other files
                        }
                    } else if (entry.isDirectory()) {
                        System.out.println("    📁 Skipping directory: " + entryName);
                    } else {
                        System.out.println("    ⚠️ Skipping unsupported file type: " + entryName);
                    }
                    
                    processZipStream.closeEntry();
                }
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            System.out.println("📊 ZIP processing complete!");
            System.out.println("   ✅ Processed: " + processedFiles + "/" + entries.size() + " files successfully");
            System.out.println("   📊 Total content blocks: " + allContent.size());
            System.out.println("   ⏱️ Total processing time: " + formatDuration(totalTime));
            
            if (totalTime >= maxProcessingTime) {
                System.out.println("   ⚠️ Processing stopped due to timeout");
            }
        } catch (Exception e) {
            System.err.println("❌ Error processing ZIP file: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to process ZIP file: " + e.getMessage(), e);
        }
        
        if (allContent.isEmpty()) {
            System.err.println("⚠️ Warning: No content could be extracted from ZIP file");
        }
        
        return allContent;
    }
    
    /**
     * Process a single file
     */
    private List<DocumentContent> processSingleFile(Document document) throws IOException {
        Path filePath = Paths.get(document.getFilePath());
        return extractContentFromFile(filePath, document.getOriginalFilename(), document);
    }
    
    /**
     * Extract content from a single file based on its type
     */
    private List<DocumentContent> extractContentFromFile(Path filePath, String filename, Document document) throws IOException {
        String fileExtension = getFileExtension(filename).toLowerCase();
        List<DocumentContent> contentList = new ArrayList<>();
        
        try {
            switch (fileExtension) {
                case ".pdf":
                    contentList = extractPdfContent(filePath, filename, document);
                    break;
                case ".doc":
                    contentList = extractDocContent(filePath, filename, document);
                    break;
                case ".docx":
                    contentList = extractDocxContent(filePath, filename, document);
                    break;
                case ".ppt":
                case ".pptx":
                    contentList = extractPowerPointContent(filePath, filename, document);
                    break;
                default:
                    // Use Tika for other file types
                    contentList = extractGenericContent(filePath, filename, document);
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error extracting content from " + filename + ": " + e.getMessage());
            // Try generic extraction as fallback
            try {
                contentList = extractGenericContent(filePath, filename, document);
            } catch (Exception fallbackError) {
                System.err.println("Fallback extraction also failed for " + filename + ": " + fallbackError.getMessage());
            }
        }
        
        return contentList;
    }
    
    /**
     * Extract content from PDF files using PDFBox
     */
    private List<DocumentContent> extractPdfContent(Path filePath, String filename, Document document) throws IOException {
        List<DocumentContent> contentList = new ArrayList<>();
        
        try (PDDocument pdfDocument = PDDocument.load(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = pdfDocument.getNumberOfPages();
            
            System.out.println("📄 Processing PDF with " + totalPages + " pages: " + filename);

            // Extract text page by page
            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                try {
                    stripper.setStartPage(pageNum);
                    stripper.setEndPage(pageNum);
                    
                    String pageText = stripper.getText(pdfDocument);
                    if (pageText != null && !pageText.trim().isEmpty()) {
                        DocumentContent content = new DocumentContent();
                        content.setDocument(document);
                        content.setContent(pageText.trim());
                        content.setPageNumber(pageNum);
                        content.setTopic(detectTopic(pageText));
                        content.setSectionTitle("Page " + pageNum);
                        contentList.add(content);
                        
                        System.out.println("  ✅ Page " + pageNum + " processed (" + pageText.trim().length() + " characters)");
                    } else {
                        System.out.println("  ⚠️ Page " + pageNum + " has no content");
                        // Still create a content entry for empty pages to maintain page count
                        DocumentContent content = new DocumentContent();
                        content.setDocument(document);
                        content.setContent("[Page " + pageNum + " - No text content]");
                        content.setPageNumber(pageNum);
                        content.setTopic("Empty Page");
                        content.setSectionTitle("Page " + pageNum);
                        contentList.add(content);
                    }
                } catch (Exception e) {
                    System.err.println("  ❌ Error processing page " + pageNum + ": " + e.getMessage());
                    // Create a placeholder for failed pages
                    DocumentContent content = new DocumentContent();
                    content.setDocument(document);
                    content.setContent("[Page " + pageNum + " - Error processing: " + e.getMessage() + "]");
                    content.setPageNumber(pageNum);
                    content.setTopic("Error Page");
                    content.setSectionTitle("Page " + pageNum);
                    contentList.add(content);
                }
            }
            
            System.out.println("📊 Total pages processed: " + contentList.size() + " out of " + totalPages);
        }
        
        return contentList;
    }
    
    /**
     * Extract content from DOC files using Apache POI
     */
    private List<DocumentContent> extractDocContent(Path filePath, String filename, Document document) throws IOException {
        List<DocumentContent> contentList = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             HWPFDocument docDocument = new HWPFDocument(fis)) {
            
            WordExtractor extractor = new WordExtractor(docDocument);
            String text = extractor.getText();
            
            if (text != null && !text.trim().isEmpty()) {
                DocumentContent content = new DocumentContent();
                content.setDocument(document);
                content.setContent(text.trim());
                content.setTopic(detectTopic(text));
                content.setSectionTitle("Document Content");
                contentList.add(content);
            }
        }
        
        return contentList;
    }
    
    /**
     * Extract content from DOCX files using Apache POI
     */
    private List<DocumentContent> extractDocxContent(Path filePath, String filename, Document document) throws IOException {
        List<DocumentContent> contentList = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             XWPFDocument docxDocument = new XWPFDocument(fis)) {
            
            StringBuilder textBuilder = new StringBuilder();
            int sectionCount = 0;
            
            for (XWPFParagraph paragraph : docxDocument.getParagraphs()) {
                String paragraphText = paragraph.getText();
                if (paragraphText != null && !paragraphText.trim().isEmpty()) {
                    textBuilder.append(paragraphText).append("\n");
                    
                    // Create content blocks for large documents
                    if (textBuilder.length() > 5000) {
                        DocumentContent content = new DocumentContent();
                        content.setDocument(document);
                        content.setContent(textBuilder.toString().trim());
                        content.setTopic(detectTopic(textBuilder.toString()));
                        content.setSectionTitle("Section " + (++sectionCount));
                        contentList.add(content);
                        textBuilder = new StringBuilder();
                    }
                }
            }
            
            // Add remaining content
            if (textBuilder.length() > 0) {
                DocumentContent content = new DocumentContent();
                content.setDocument(document);
                content.setContent(textBuilder.toString().trim());
                content.setTopic(detectTopic(textBuilder.toString()));
                content.setSectionTitle("Section " + (++sectionCount));
                contentList.add(content);
            }
        }
        
        return contentList;
    }
    
    /**
     * Extract content from PowerPoint files using Apache POI
     */
    private List<DocumentContent> extractPowerPointContent(Path filePath, String filename, Document document) throws IOException {
        List<DocumentContent> contentList = new ArrayList<>();
        
        try {
            // Use Tika for PowerPoint files as it handles them well
            String text = tika.parseToString(filePath.toFile());
            
            if (text != null && !text.trim().isEmpty()) {
                // Split by slide indicators
                String[] slides = text.split("Slide \\d+");
                
                for (int i = 0; i < slides.length; i++) {
                    String slideText = slides[i].trim();
                    if (!slideText.isEmpty()) {
                        DocumentContent content = new DocumentContent();
                        content.setDocument(document);
                        content.setContent(slideText);
                        content.setSlideNumber(i + 1);
                        content.setTopic(detectTopic(slideText));
                        content.setSectionTitle("Slide " + (i + 1));
                        contentList.add(content);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting PowerPoint content: " + e.getMessage());
        }
        
        return contentList;
    }
    
    /**
     * Extract content using Apache Tika (generic method)
     */
    private List<DocumentContent> extractGenericContent(Path filePath, String filename, Document document) throws IOException, TikaException {
        List<DocumentContent> contentList = new ArrayList<>();
        
        try {
            String text = tika.parseToString(filePath.toFile());
            
            if (text != null && !text.trim().isEmpty()) {
                DocumentContent content = new DocumentContent();
                content.setDocument(document);
                content.setContent(text.trim());
                content.setTopic(detectTopic(text));
                content.setSectionTitle("Document Content");
                contentList.add(content);
            }
        } catch (Exception e) {
            System.err.println("Error extracting generic content: " + e.getMessage());
        }
        
        return contentList;
    }
    
    /**
     * Extract a file from ZIP to temporary location
     */
    private Path extractZipEntry(ZipInputStream zipInputStream, String entryName) throws IOException {
        // Create a unique temporary file with proper extension
        String extension = getFileExtension(entryName);
        String prefix = "extracted_" + System.currentTimeMillis() + "_";
        Path tempFile = Files.createTempFile(prefix, extension);
        
        System.out.println("    📁 Extracting to temp file: " + tempFile);
        
        try (OutputStream outputStream = Files.newOutputStream(tempFile)) {
            byte[] buffer = new byte[32768]; // 32KB buffer for better performance
            int length;
            long totalBytes = 0;
            long lastLogTime = System.currentTimeMillis();
            
            while ((length = zipInputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
                totalBytes += length;
                
                // Log progress for large files (every 5 seconds)
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastLogTime > 5000 && totalBytes > 1024 * 1024) { // Log every 5s for files > 1MB
                    System.out.println("      📊 Extraction progress: " + formatFileSize(totalBytes));
                    lastLogTime = currentTime;
                }
            }
            
            System.out.println("    📊 Extracted " + formatFileSize(totalBytes) + " to " + tempFile);
        } catch (Exception e) {
            // Clean up temp file if extraction fails
            try {
                Files.deleteIfExists(tempFile);
            } catch (Exception cleanupError) {
                System.err.println("    ⚠️ Could not cleanup temp file: " + cleanupError.getMessage());
            }
            throw new IOException("Failed to extract ZIP entry " + entryName + ": " + e.getMessage(), e);
        }
        
        return tempFile;
    }
    
    /**
     * Detect topic from text content
     */
    private String detectTopic(String text) {
        // Simple topic detection - can be enhanced with NLP later
        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.length() > 10 && line.length() < 100 && 
                (line.endsWith(":") || Character.isUpperCase(line.charAt(0)))) {
                return line.replace(":", "").trim();
            }
        }
        return "General Content";
    }
    
    /**
     * Check if file type is supported
     */
    private boolean isSupportedFileType(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }
        
        String extension = getFileExtension(filename).toLowerCase();
        boolean isSupported = extension.equals(".pdf") || 
                             extension.equals(".doc") || 
                             extension.equals(".docx") || 
                             extension.equals(".ppt") || 
                             extension.equals(".pptx") ||
                             extension.equals(".txt") ||
                             extension.equals(".rtf");
        
        System.out.println("    🔍 File type check: " + filename + " -> " + extension + " -> " + (isSupported ? "✅ Supported" : "❌ Not supported"));
        
        return isSupported;
    }
    
    /**
     * Update document status
     */
    private Document updateDocumentStatus(Long id, DocumentStatus status) {
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
     * Format file size in human-readable format
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * Format duration in human-readable format
     */
    private String formatDuration(long milliseconds) {
        if (milliseconds < 1000) return milliseconds + "ms";
        if (milliseconds < 60000) return (milliseconds / 1000) + "s";
        long minutes = milliseconds / 60000;
        long seconds = (milliseconds % 60000) / 1000;
        return minutes + "m " + seconds + "s";
    }
}
