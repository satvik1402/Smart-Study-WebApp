package com.smartstudy.controller;

import com.smartstudy.service.GeminiService;
import com.smartstudy.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for AI-powered features
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIController {
    
    @Autowired
    private GeminiService geminiService;
    
    @Autowired
    private AnalyticsService analyticsService;
    
    /**
     * Generate AI-powered answer to a question
     */
    @PostMapping("/qa")
    public ResponseEntity<Map<String, Object>> generateAnswer(@RequestBody Map<String, Object> request) {
        try {
            try { analyticsService.incrementAiInteractions(); } catch (Exception ignore) {}
            String question = (String) request.get("question");
            Integer maxResults = (Integer) request.getOrDefault("maxResults", 10);
            
            if (question == null || question.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Question is required"));
            }
            
            String answer = geminiService.generateAnswer(question, maxResults);
            
            return ResponseEntity.ok(Map.of(
                "question", question,
                "answer", answer,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate answer: " + e.getMessage()));
        }
    }
    
    /**
     * Generate summary for a topic
     */
    @PostMapping("/summarize")
    public ResponseEntity<Map<String, Object>> generateSummary(@RequestBody Map<String, Object> request) {
        try {
            try { analyticsService.incrementAiInteractions(); } catch (Exception ignore) {}
            String topic = (String) request.get("topic");
            Integer maxResults = (Integer) request.getOrDefault("maxResults", 15);
            
            if (topic == null || topic.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Topic is required"));
            }
            
            String summary = geminiService.generateSummary(topic, maxResults);
            
            return ResponseEntity.ok(Map.of(
                "topic", topic,
                "summary", summary,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate summary: " + e.getMessage()));
        }
    }
    
    /**
     * Generate quiz questions
     */
    @PostMapping("/quiz")
    public ResponseEntity<Map<String, Object>> generateQuiz(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("🎯 Quiz generation request: " + request);
            try { analyticsService.incrementAiInteractions(); } catch (Exception ignore) {}
            
            @SuppressWarnings("unchecked")
            List<Object> documentIdsObj = (List<Object>) request.get("documentIds");
            Integer questionCount = (Integer) request.getOrDefault("questionCount", 10);
            String difficulty = (String) request.getOrDefault("difficulty", "MEDIUM");
            @SuppressWarnings("unchecked")
            List<String> questionTypes = (List<String>) request.getOrDefault("questionTypes", 
                    List.of("MCQ", "TRUE_FALSE", "FILL_BLANK"));
            
            if (documentIdsObj == null || documentIdsObj.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Document IDs are required"));
            }
            
            // Convert document IDs to Long (handle both Integer and Long)
            List<Long> documentIds = documentIdsObj.stream()
                    .map(id -> {
                        if (id instanceof Integer) {
                            return ((Integer) id).longValue();
                        } else if (id instanceof Long) {
                            return (Long) id;
                        } else {
                            throw new IllegalArgumentException("Invalid document ID type: " + id.getClass());
                        }
                    })
                    .collect(Collectors.toList());
            
            System.out.println("🎯 Converted document IDs: " + documentIds);
            
            List<GeminiService.QuizQuestion> questions = geminiService.generateQuizQuestions(
                    documentIds, questionCount, difficulty, questionTypes);
            
            return ResponseEntity.ok(Map.of(
                "questions", questions,
                "totalQuestions", questions.size(),
                "difficulty", difficulty,
                "questionTypes", questionTypes,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            System.err.println("❌ Quiz generation error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate quiz: " + e.getMessage()));
        }
    }
    
    /**
     * Generate flashcards
     */
    @PostMapping("/flashcards")
    public ResponseEntity<Map<String, Object>> generateFlashcards(@RequestBody Map<String, Object> request) {
        try {
            try { analyticsService.incrementAiInteractions(); } catch (Exception ignore) {}
            String topic = (String) request.get("topic");
            Integer cardCount = (Integer) request.getOrDefault("cardCount", 10);
            Long documentId = request.get("documentId") != null ? Long.valueOf(request.get("documentId").toString()) : null;

            if (topic == null || topic.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Topic is required"));
            }
            if (documentId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Document ID is required"));
            }

            List<GeminiService.Flashcard> flashcards = geminiService.generateFlashcards(documentId, topic, cardCount);

            return ResponseEntity.ok(Map.of(
                "topic", topic,
                "flashcards", flashcards,
                "totalCards", flashcards.size(),
                "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate flashcards: " + e.getMessage()));
        }
    }
    
    /**
     * Extract key concepts from documents
     */
    @PostMapping("/concepts")
    public ResponseEntity<Map<String, Object>> extractKeyConcepts(@RequestBody Map<String, Object> request) {
        try {
            try { analyticsService.incrementAiInteractions(); } catch (Exception ignore) {}
            Integer maxResults = (Integer) request.getOrDefault("maxResults", 20);
            Long documentId = request.get("documentId") != null ? Long.valueOf(request.get("documentId").toString()) : null;

            if (documentId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Document ID is required"));
            }

            List<String> concepts = geminiService.extractKeyConcepts(documentId, maxResults);

            return ResponseEntity.ok(Map.of(
                "concepts", concepts,
                "totalConcepts", concepts.size(),
                "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to extract concepts: " + e.getMessage()));
        }
    }
    
    /**
     * Health check for AI service
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI service is running");
    }
    
    /**
     * Test AI service with a simple prompt
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testAI(@RequestBody Map<String, Object> request) {
        try {
            String testPrompt = (String) request.getOrDefault("prompt", "Hello, how are you?");
            
            // Use a simple test call to verify API connectivity
            String response = geminiService.callGeminiAPI("Please respond with 'AI service is working correctly' if you can read this.");
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "AI service is working correctly",
                "testPrompt", testPrompt,
                "response", response,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                        "status", "error",
                        "message", "AI service test failed",
                        "error", e.getMessage(),
                        "timestamp", System.currentTimeMillis()
                    ));
        }
    }
}
