package com.smartstudy.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity class representing a generated quiz
 */
@Entity
@Table(name = "quizzes")
public class Quiz {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "title", nullable = false)
    private String title;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "document_ids", columnDefinition = "JSON")
    private String documentIds; // JSON array of document IDs
    
    @Column(name = "question_count")
    private Integer questionCount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty")
    private DifficultyLevel difficulty;
    
    @Column(name = "question_types", columnDefinition = "JSON")
    private String questionTypes; // JSON array of question types
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;
    
    @Column(name = "passing_score")
    private Integer passingScore;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    // Constructors
    public Quiz() {
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
    }
    
    public Quiz(String title, String description, Integer questionCount, DifficultyLevel difficulty) {
        this();
        this.title = title;
        this.description = description;
        this.questionCount = questionCount;
        this.difficulty = difficulty;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDocumentIds() {
        return documentIds;
    }
    
    public void setDocumentIds(String documentIds) {
        this.documentIds = documentIds;
    }
    
    public Integer getQuestionCount() {
        return questionCount;
    }
    
    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }
    
    public DifficultyLevel getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }
    
    public String getQuestionTypes() {
        return questionTypes;
    }
    
    public void setQuestionTypes(String questionTypes) {
        this.questionTypes = questionTypes;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Integer getTimeLimitMinutes() {
        return timeLimitMinutes;
    }
    
    public void setTimeLimitMinutes(Integer timeLimitMinutes) {
        this.timeLimitMinutes = timeLimitMinutes;
    }
    
    public Integer getPassingScore() {
        return passingScore;
    }
    
    public void setPassingScore(Integer passingScore) {
        this.passingScore = passingScore;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    @Override
    public String toString() {
        return "Quiz{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", questionCount=" + questionCount +
                ", difficulty=" + difficulty +
                ", createdAt=" + createdAt +
                '}';
    }
    
    /**
     * Enum for quiz difficulty levels
     */
    public enum DifficultyLevel {
        EASY,
        MEDIUM,
        HARD
    }
}

