package com.smartstudy.repository;

import com.smartstudy.model.Quiz;
import com.smartstudy.model.Quiz.DifficultyLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Quiz entity operations
 */
@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    
    /**
     * Find quizzes by difficulty level
     */
    List<Quiz> findByDifficulty(DifficultyLevel difficulty);
    
    /**
     * Find active quizzes
     */
    List<Quiz> findByIsActiveTrue();
    
    /**
     * Find quizzes by difficulty and active status
     */
    List<Quiz> findByDifficultyAndIsActiveTrue(DifficultyLevel difficulty);
    
    /**
     * Find quizzes created after a specific date
     */
    List<Quiz> findByCreatedAtAfter(LocalDateTime date);
    
    /**
     * Find quizzes with question count greater than specified value
     */
    List<Quiz> findByQuestionCountGreaterThan(Integer questionCount);
    
    /**
     * Find quizzes by title containing text (case-insensitive)
     */
    List<Quiz> findByTitleContainingIgnoreCase(String title);
    
    /**
     * Find quizzes by description containing text (case-insensitive)
     */
    List<Quiz> findByDescriptionContainingIgnoreCase(String description);
    
    /**
     * Find quizzes created in the last N days
     */
    @Query("SELECT q FROM Quiz q WHERE q.createdAt >= :startDate ORDER BY q.createdAt DESC")
    List<Quiz> findRecentQuizzes(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Count quizzes by difficulty level
     */
    long countByDifficulty(DifficultyLevel difficulty);
    
    /**
     * Count active quizzes
     */
    long countByIsActiveTrue();
    
    /**
     * Find quizzes with time limit
     */
    @Query("SELECT q FROM Quiz q WHERE q.timeLimitMinutes IS NOT NULL AND q.timeLimitMinutes > 0")
    List<Quiz> findQuizzesWithTimeLimit();
    
    /**
     * Find quizzes by document IDs (JSON contains)
     */
    @Query("SELECT q FROM Quiz q WHERE q.documentIds LIKE %:documentId%")
    List<Quiz> findByDocumentId(@Param("documentId") String documentId);
    
    /**
     * Get quiz statistics
     */
    @Query("SELECT q.difficulty, COUNT(q), AVG(q.questionCount) FROM Quiz q GROUP BY q.difficulty")
    List<Object[]> getQuizStatistics();
    
    /**
     * Find quizzes with passing score
     */
    @Query("SELECT q FROM Quiz q WHERE q.passingScore IS NOT NULL AND q.passingScore > 0")
    List<Quiz> findQuizzesWithPassingScore();
    
    /**
     * Find quizzes by multiple difficulty levels
     */
    @Query("SELECT q FROM Quiz q WHERE q.difficulty IN :difficulties")
    List<Quiz> findByDifficulties(@Param("difficulties") List<DifficultyLevel> difficulties);
    
    /**
     * Find quizzes with specific question types
     */
    @Query("SELECT q FROM Quiz q WHERE q.questionTypes LIKE %:questionType%")
    List<Quiz> findByQuestionType(@Param("questionType") String questionType);
}

