package com.smartstudy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for Google Gemini AI integration
 */
@Service
public class GeminiService {
    
    @Autowired
    private SearchService searchService;
    
    @Value("${gemini.api.key}")
    private String apiKey;
    
    @Value("${gemini.api.url}")
    private String apiUrl;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public GeminiService() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Generate AI-powered answer to a question based on document content
     */
    public String generateAnswer(String question, int maxResults) throws Exception {
        // Always get ALL content from all documents to give Gemini full context
        List<SearchService.SearchResult> searchResults = searchService.search("", 1000);
        
        if (searchResults.isEmpty()) {
            return "I couldn't find any information in the uploaded documents. Please make sure documents have been uploaded and processed.";
        }
        
        System.out.println("🔍 Found " + searchResults.size() + " content blocks for question: " + question);
        
        // Prepare context from search results
        String context = prepareContextFromSearchResults(searchResults);
        
        // Create a more intelligent prompt that encourages creative thinking
        String prompt = String.format("""
            You are an intelligent AI assistant helping a student with their study materials. 
            The student has asked: "%s"
            
            Below is the content from their uploaded documents. Please:
            
            1. **Think creatively** - Even if the exact answer isn't in the documents, use your knowledge to provide helpful, relevant information
            2. **Always provide page references** - When you reference information, cite the exact document name and page number
            3. **Be helpful** - If the question is related to the subject matter but not directly covered, provide relevant insights
            4. **Combine document knowledge with general knowledge** - Use both the document content and your understanding to give comprehensive answers
            5. **Validate with sources** - Always mention which pages support your answers
            
            Document Content:
            %s
            
            Remember: Think like a knowledgeable tutor who can connect concepts and provide insights beyond just what's explicitly written.
            """, question, context);
        
        return callGeminiAPI(prompt);
    }
    
    /**
     * Generate summary for a specific topic or concept
     */
    public String generateSummary(String topic, int maxResults) throws Exception {
        // Always get ALL content from all documents to give Gemini full context
        List<SearchService.SearchResult> searchResults = searchService.search("", 1000);
        
        if (searchResults.isEmpty()) {
            return "I couldn't find any information in the uploaded documents. Please make sure documents have been uploaded and processed.";
        }
        
        System.out.println("🔍 Found " + searchResults.size() + " content blocks for topic: " + topic);
        
        // Prepare context from search results
        String context = prepareContextFromSearchResults(searchResults);
        
        // Create a smarter prompt that understands natural language and context
        String prompt = String.format("""
            You are an intelligent AI assistant helping a student summarize their study materials.
            
            The student wants to summarize: "%s"
            
            This could be:
            - A single word (e.g., "clustering", "databases")
            - A phrase (e.g., "clustering in dbms", "database management systems")
            - A sentence (e.g., "What is clustering in database systems?")
            - A concept (e.g., "machine learning algorithms")
            
            Your task:
            1. **Understand the intent** - Figure out what the student really wants to know about
            2. **Find relevant content** - Look through the documents for related information
            3. **Be flexible** - Don't just look for exact matches, find related concepts
            4. **Provide comprehensive summary** - Include key points, definitions, examples
            5. **Always cite sources** - Reference specific document names and page numbers
            6. **Be helpful** - Even if the exact topic isn't found, provide related information
            
            Document Content:
            %s
            
            Remember: Think like a knowledgeable tutor who can understand what students are asking for, even when they don't use the exact terminology.
            """, topic, context);
        
        return callGeminiAPI(prompt);
    }
    
    /**
     * Generate quiz questions from document content
     */
    public List<QuizQuestion> generateQuizQuestions(List<Long> documentIds, int questionCount, 
                                                   String difficulty, List<String> questionTypes) throws Exception {
        // Get content from specified documents
        StringBuilder allContent = new StringBuilder();
        for (Long documentId : documentIds) {
            try {
                List<SearchService.SearchResult> results = searchService.search("", 100); // Get more results
                for (SearchService.SearchResult result : results) {
                    if (result.getDocumentId().equals(documentId)) {
                        allContent.append("Document: ").append(result.getFilename())
                                .append(" (Page/Slide: ").append(result.getPageNumber() != null ? 
                                        "Page " + result.getPageNumber().toString() : 
                                        result.getSlideNumber() != null ? "Slide " + result.getSlideNumber().toString() : "N/A")
                                .append(")\n")
                                .append(result.getContent()).append("\n\n");
                    }
                }
            } catch (Exception e) {
                System.err.println("Error getting content for document " + documentId + ": " + e.getMessage());
            }
        }
        
        if (allContent.length() == 0) {
            throw new RuntimeException("No content found in the specified documents");
        }
        
        // Create prompt for quiz generation
        String typesStr = String.join(", ", questionTypes);
        String prompt = String.format("""
            Based on the following document content, generate %d %s quiz questions.
            Include a mix of question types: %s
            
            For each question, provide:
            1. Question text
            2. Question type (MCQ, True/False, Fill-in-the-blank)
            3. Correct answer
            4. For MCQs: 4 options (A, B, C, D)
            5. Explanation of the correct answer
            6. Source reference (document name and page/slide)
            
            Format the response as JSON with the following structure:
            {
                "questions": [
                    {
                        "question": "Question text",
                        "type": "MCQ|TRUE_FALSE|FILL_BLANK",
                        "correctAnswer": "Correct answer",
                        "options": ["A", "B", "C", "D"],
                        "explanation": "Explanation",
                        "source": "Document name - Page/Slide X"
                    }
                ]
            }
            
            Document Content:
            %s
            """, questionCount, difficulty.toLowerCase(), typesStr, allContent.toString());
        
        String response = callGeminiAPI(prompt);
        return parseQuizQuestions(response);
    }
    
    /**
     * Generate flashcards from document content
     */
    public List<Flashcard> generateFlashcards(Long documentId, String topic, int cardCount) throws Exception {
        // Fetch all content for the document
        List<SearchService.SearchResult> searchResults = searchService.search("", 1000);
        List<SearchService.SearchResult> filteredResults = searchResults.stream()
                .filter(result -> result.getDocumentId().equals(documentId) && (topic == null || topic.isEmpty() || result.getContent().toLowerCase().contains(topic.toLowerCase())))
                .collect(Collectors.toList());

        // Fallback: If no content matches the topic, use all content from the document
        if (filteredResults.isEmpty()) {
            filteredResults = searchResults.stream()
                .filter(result -> result.getDocumentId().equals(documentId))
                .collect(Collectors.toList());
        }

        if (filteredResults.isEmpty()) {
            throw new RuntimeException("No content found for document: " + documentId);
        }

        String context = prepareContextFromSearchResults(filteredResults);

        String prompt = String.format("""
            Based on the following document content, generate %d flashcards for the topic: %s
            
            For each flashcard, provide:
            1. Front side (question or concept)
            2. Back side (answer or explanation)
            3. Source reference
            
            Format the response as JSON:
            {
                "flashcards": [
                    {
                        "front": "Question/Concept",
                        "back": "Answer/Explanation",
                        "source": "Document name - Page/Slide X"
                    }
                ]
            }
            
            Document Content:
            %s
            """, cardCount, topic, context);

        String response = callGeminiAPI(prompt);
        return parseFlashcards(response);
    }

    // Original method retained for backward compatibility
    public List<Flashcard> generateFlashcards(String topic, int cardCount) throws Exception {
        // Search for content related to the topic
        List<SearchService.SearchResult> searchResults = searchService.search(topic, 20);
        
        if (searchResults.isEmpty()) {
            throw new RuntimeException("No content found for topic: " + topic);
        }
        
        String context = prepareContextFromSearchResults(searchResults);
        
        String prompt = String.format("""
            Based on the following document content, generate %d flashcards for the topic: %s
            
            For each flashcard, provide:
            1. Front side (question or concept)
            2. Back side (answer or explanation)
            3. Source reference
            
            Format the response as JSON:
            {
                "flashcards": [
                    {
                        "front": "Question/Concept",
                        "back": "Answer/Explanation",
                        "source": "Document name - Page/Slide X"
                    }
                ]
            }
            
            Document Content:
            %s
            """, cardCount, topic, context);
        
        String response = callGeminiAPI(prompt);
        return parseFlashcards(response);
    }
    
    /**
     * Extract key concepts from document content
     */
    public List<String> extractKeyConcepts(Long documentId, int maxResults) throws Exception {
        // Fetch content only from the specified document
        List<SearchService.SearchResult> searchResults = searchService.search("", 1000);
        List<SearchService.SearchResult> filteredResults = searchResults.stream()
                .filter(result -> result.getDocumentId().equals(documentId))
                .limit(maxResults)
                .collect(Collectors.toList());

        if (filteredResults.isEmpty()) {
            return List.of();
        }

        String context = prepareContextFromSearchResults(filteredResults);

        String prompt = String.format("""
            Based on the following document content, extract the main key concepts, topics, and important terms.
            Return them as a simple list, one concept per line.
            Focus on academic subjects, technical terms, and important concepts.
            
            Document Content:
            %s
            """, context);

        String response = callGeminiAPI(prompt);
        return parseKeyConcepts(response);
    }

    // Original method retained for backward compatibility
    public List<String> extractKeyConcepts(int maxResults) throws Exception {
        List<SearchService.SearchResult> searchResults = searchService.search("", maxResults);
        
        if (searchResults.isEmpty()) {
            return List.of();
        }
        
        String context = prepareContextFromSearchResults(searchResults);
        
        String prompt = String.format("""
            Based on the following document content, extract the main key concepts, topics, and important terms.
            Return them as a simple list, one concept per line.
            Focus on academic subjects, technical terms, and important concepts.
            
            Document Content:
            %s
            """, context);
        
        String response = callGeminiAPI(prompt);
        return parseKeyConcepts(response);
    }
    
    /**
     * Call Gemini API with the given prompt
     */
    public String callGeminiAPI(String prompt) throws Exception {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                System.out.println("🤖 Calling Gemini API (attempt " + (retryCount + 1) + "/" + maxRetries + ")");
                
                // Prepare request body
                Map<String, Object> requestBody = Map.of(
                    "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                    )),
                    "generationConfig", Map.of(
                        "temperature", 0.7,
                        "topK", 40,
                        "topP", 0.95,
                        "maxOutputTokens", 2048
                    )
                );
                
                // Make API call
                String response = webClient.post()
                        .uri(apiUrl + "?key=" + apiKey)
                        .bodyValue(requestBody)
                        .retrieve()
                        .onStatus(status -> status.is5xxServerError() || status.is4xxClientError(),
                                clientResponse -> clientResponse.bodyToMono(String.class)
                                        .flatMap(errorBody -> Mono.error(new RuntimeException("API Error: " + clientResponse.statusCode() + " - " + errorBody))))
                        .bodyToMono(String.class)
                        .block();
                
                if (response == null) {
                    throw new RuntimeException("No response from Gemini API");
                }
                
                // Parse response
                JsonNode responseJson = objectMapper.readTree(response);
                JsonNode candidates = responseJson.get("candidates");
                
                if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                    JsonNode content = candidates.get(0).get("content");
                    if (content != null) {
                        JsonNode parts = content.get("parts");
                        if (parts != null && parts.isArray() && parts.size() > 0) {
                            String result = parts.get(0).get("text").asText();
                            System.out.println("✅ Gemini API call successful");
                            return result;
                        }
                    }
                }
                
                throw new RuntimeException("Invalid response format from Gemini API");
                
            } catch (Exception e) {
                retryCount++;
                System.err.println("❌ Gemini API call failed (attempt " + retryCount + "/" + maxRetries + "): " + e.getMessage());
                
                if (retryCount >= maxRetries) {
                    // If all retries failed, provide a fallback response
                    System.err.println("🚨 All Gemini API retries failed, providing fallback response");
                    return "I'm experiencing technical difficulties with the AI service. Please try again later, or check the search results directly to find the information you need.";
                }
                
                // Wait before retrying
                try {
                    Thread.sleep(2000 * retryCount); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        throw new RuntimeException("Failed to generate AI response after " + maxRetries + " attempts");
    }
    
    /**
     * Prepare context from search results
     */
    private String prepareContextFromSearchResults(List<SearchService.SearchResult> results) {
        System.out.println("📝 Preparing context from " + results.size() + " search results");
        
        // Log the first few results for debugging
        for (int i = 0; i < Math.min(3, results.size()); i++) {
            SearchService.SearchResult result = results.get(i);
            System.out.println("  📄 Result " + (i + 1) + ": " + result.getFilename() + 
                             " - Page " + result.getPageNumber() + 
                             " - Content length: " + (result.getContent() != null ? result.getContent().length() : 0));
        }
        
        return results.stream()
                .map(result -> String.format("""
                    Source: %s
                    Location: %s
                    Topic: %s
                    Content: %s
                    ---
                    """, 
                    result.getFilename(),
                    result.getPageNumber() != null ? "Page " + result.getPageNumber().toString() : 
                            result.getSlideNumber() != null ? "Slide " + result.getSlideNumber().toString() : "N/A",
                    result.getTopic() != null ? result.getTopic() : "General Content",
                    result.getContent()))
                .collect(Collectors.joining("\n"));
    }
    
    /**
     * Parse quiz questions from AI response
     */
    private List<QuizQuestion> parseQuizQuestions(String response) {
        try {
            // Try to extract JSON from response
            int jsonStart = response.indexOf("{");
            int jsonEnd = response.lastIndexOf("}") + 1;
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonStr = response.substring(jsonStart, jsonEnd);
                JsonNode root = objectMapper.readTree(jsonStr);
                JsonNode questions = root.get("questions");
                
                if (questions != null && questions.isArray()) {
                    List<QuizQuestion> quizQuestions = new java.util.ArrayList<>();
                    for (JsonNode questionNode : questions) {
                        QuizQuestion question = new QuizQuestion();
                        question.setQuestion(questionNode.get("question").asText());
                        question.setType(questionNode.get("type").asText());
                        question.setCorrectAnswer(questionNode.get("correctAnswer").asText());
                        question.setExplanation(questionNode.get("explanation").asText());
                        question.setSource(questionNode.get("source").asText());
                        
                        JsonNode options = questionNode.get("options");
                        if (options != null && options.isArray()) {
                            List<String> optionsList = new java.util.ArrayList<>();
                            for (JsonNode option : options) {
                                optionsList.add(option.asText());
                            }
                            question.setOptions(optionsList);
                        }
                        
                        quizQuestions.add(question);
                    }
                    return quizQuestions;
                }
            }
            
            // Fallback: return a simple question if parsing fails
            return List.of(new QuizQuestion("Failed to parse quiz questions", "MCQ", "Error", 
                    "Please try again", "Error", List.of("A", "B", "C", "D")));
            
        } catch (Exception e) {
            System.err.println("Error parsing quiz questions: " + e.getMessage());
            return List.of(new QuizQuestion("Failed to parse quiz questions", "MCQ", "Error", 
                    "Please try again", "Error", List.of("A", "B", "C", "D")));
        }
    }
    
    /**
     * Parse flashcards from AI response
     */
    private List<Flashcard> parseFlashcards(String response) {
        try {
            int jsonStart = response.indexOf("{");
            int jsonEnd = response.lastIndexOf("}") + 1;
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonStr = response.substring(jsonStart, jsonEnd);
                JsonNode root = objectMapper.readTree(jsonStr);
                JsonNode flashcards = root.get("flashcards");
                
                if (flashcards != null && flashcards.isArray()) {
                    List<Flashcard> flashcardList = new java.util.ArrayList<>();
                    for (JsonNode cardNode : flashcards) {
                        Flashcard card = new Flashcard();
                        card.setFront(cardNode.get("front").asText());
                        card.setBack(cardNode.get("back").asText());
                        card.setSource(cardNode.get("source").asText());
                        flashcardList.add(card);
                    }
                    return flashcardList;
                }
            }
            
            return List.of(new Flashcard("Failed to parse flashcards", "Error", "Error"));
            
        } catch (Exception e) {
            System.err.println("Error parsing flashcards: " + e.getMessage());
            return List.of(new Flashcard("Failed to parse flashcards", "Error", "Error"));
        }
    }
    
    /**
     * Parse key concepts from AI response
     */
    private List<String> parseKeyConcepts(String response) {
        try {
            return response.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#") && !line.startsWith("-"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error parsing key concepts: " + e.getMessage());
            return List.of("Failed to extract key concepts");
        }
    }
    
    /**
     * Quiz Question class
     */
    public static class QuizQuestion {
        private String question;
        private String type;
        private String correctAnswer;
        private List<String> options;
        private String explanation;
        private String source;
        
        public QuizQuestion() {}
        
        public QuizQuestion(String question, String type, String correctAnswer, 
                          String explanation, String source, List<String> options) {
            this.question = question;
            this.type = type;
            this.correctAnswer = correctAnswer;
            this.explanation = explanation;
            this.source = source;
            this.options = options;
        }
        
        // Getters and setters
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getCorrectAnswer() { return correctAnswer; }
        public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
        
        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }
        
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
        
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    }
    
    /**
     * Flashcard class
     */
    public static class Flashcard {
        private String front;
        private String back;
        private String source;
        
        public Flashcard() {}
        
        public Flashcard(String front, String back, String source) {
            this.front = front;
            this.back = back;
            this.source = source;
        }
        
        // Getters and setters
        public String getFront() { return front; }
        public void setFront(String front) { this.front = front; }
        
        public String getBack() { return back; }
        public void setBack(String back) { this.back = back; }
        
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    }
}
