console.log('quiz.js loaded');
// Quiz Generator JavaScript
// Use window.API_BASE, do not redeclare. Set globally elsewhere if needed.
// Global variables
let currentQuiz = null;
let currentQuestionIndex = 0;
let quizAnswers = {};
let quizTimer = null;
let timeRemaining = 0;
// DOM elements
const quizCreation = document.getElementById('quizCreation');
const quizTaking = document.getElementById('quizTaking');
const quizResults = document.getElementById('quizResults');
const quizzesGrid = document.getElementById('quizzesGrid');
// Quiz creation elements
const quizTitle = document.getElementById('quizTitle');
const quizTopic = document.getElementById('quizTopic');
const quizDocument = document.getElementById('quizDocument');
const refreshDocumentsBtn = document.getElementById('refreshDocuments');
const documentLoadingSpinner = document.getElementById('documentLoadingSpinner');
const notificationDiv = document.getElementById('quizNotification');
const questionCount = document.getElementById('questionCount');
const difficulty = document.getElementById('difficulty');
const timeLimit = document.getElementById('timeLimit');
const passingScore = document.getElementById('passingScore');
const generateQuizBtn = document.getElementById('generateQuiz');
const previewQuizBtn = document.getElementById('previewQuiz');
// Quiz taking elements
const currentQuizTitle = document.getElementById('currentQuizTitle');
const questionProgress = document.getElementById('questionProgress');
const timeRemainingEl = document.getElementById('timeRemaining');
const progressFill = document.getElementById('progressFill');
const questionContainer = document.getElementById('questionContainer');
const prevQuestionBtn = document.getElementById('prevQuestion');
const nextQuestionBtn = document.getElementById('nextQuestion');
const submitQuizBtn = document.getElementById('submitQuiz');
const exitQuizBtn = document.getElementById('exitQuiz');
// Quiz results elements
const finalScore = document.getElementById('finalScore');
const scoreText = document.getElementById('scoreText');
const scoreDetails = document.getElementById('scoreDetails');
const correctAnswers = document.getElementById('correctAnswers');
const incorrectAnswers = document.getElementById('incorrectAnswers');
const timeTaken = document.getElementById('timeTaken');
const accuracy = document.getElementById('accuracy');
const questionReview = document.getElementById('questionReview');
const retakeQuizBtn = document.getElementById('retakeQuiz');
const newQuizBtn = document.getElementById('newQuiz');
const downloadResultsBtn = document.getElementById('downloadResults');
// Initialize the page
document.addEventListener('DOMContentLoaded', function() {
    initializeEventListeners();
    loadDocuments();
    if (refreshDocumentsBtn) {
        refreshDocumentsBtn.addEventListener('click', function() {
            loadDocuments();
        });
    }
    // Render any saved recent quizzes
    renderRecentQuizzes();
});
// Initialize event listeners
function initializeEventListeners() {
    if (refreshDocumentsBtn) {
        refreshDocumentsBtn.addEventListener('click', function() {
            loadDocuments();
        });
    }
    // Quiz creation
    generateQuizBtn.addEventListener('click', generateQuiz);
    previewQuizBtn.addEventListener('click', previewQuiz);
    // Quiz taking
    prevQuestionBtn.addEventListener('click', previousQuestion);
    nextQuestionBtn.addEventListener('click', nextQuestion);
    submitQuizBtn.addEventListener('click', submitQuiz);
    exitQuizBtn.addEventListener('click', exitQuiz);
    // Quiz results
    retakeQuizBtn.addEventListener('click', retakeQuiz);
    newQuizBtn.addEventListener('click', createNewQuiz);
    downloadResultsBtn.addEventListener('click', downloadResults);
    // Question type checkboxes
    document.querySelectorAll('input[type="checkbox"]').forEach(checkbox => {
        checkbox.addEventListener('change', updatePreviewButton);
    });
}
// Load documents for quiz creation
async function loadDocuments() {
    try {
        quizDocument.setAttribute('aria-busy', 'true');
        quizDocument.innerHTML = '<option value="">-- Select a document --</option>';
        notificationDiv.textContent = '';
        
        // Try /api/documents, fallback to /api/test/documents
        let response = await fetch(`${API_BASE}/documents`);
        if (!response.ok) {
            response = await fetch(`${API_BASE}/test/documents`);
        }
        if (response.ok) {
            const documents = await response.json();
            console.log('Documents fetched for quiz:', documents);
            if (Array.isArray(documents) && documents.length > 0) {
                documents.forEach(doc => {
                    const option = document.createElement('option');
                    option.value = doc.id;
                    option.textContent = doc.originalFilename || doc.filename;
                    quizDocument.appendChild(option);
                });
                notificationDiv.textContent = '';
            } else {
                notificationDiv.textContent = 'No documents available. Please upload some documents first.';
            }
        } else {
            notificationDiv.textContent = 'Failed to load documents. Please try again.';
        }
    } catch (error) {
        notificationDiv.textContent = 'Error loading documents. Please check your connection.';
    } finally {
        quizDocument.setAttribute('aria-busy', 'false');
    }
}
// Update preview button state
function updatePreviewButton() {
    const selectedDocs = quizDocument && quizDocument.value ? [quizDocument.value] : [];
    previewQuizBtn.disabled = selectedDocs.length === 0;
}
// Generate quiz
async function generateQuiz() {
    const title = quizTitle.value.trim();
    const topic = quizTopic.value.trim();
    const documentId = quizDocument.value;
    // Only allow MCQ and TRUE_FALSE
    // Map UI values to backend expected identifiers
    const questionTypes = Array.from(document.querySelectorAll('input[type="checkbox"]:checked'))
        .map(cb => cb.value)
        .filter(type => type === 'multiple_choice' || type === 'true_false')
        .map(type => type === 'multiple_choice' ? 'MCQ' : 'TRUE_FALSE');
    if (!title) {
        alert('Please enter a quiz title');
        return;
    }
    if (!documentId) {
        alert('Please select a document');
        return;
    }
    if (questionTypes.length === 0) {
        alert('Please select at least one question type');
        return;
    }
    generateQuizBtn.disabled = true;
    generateQuizBtn.innerHTML = '<i class="bx bx-loader-alt"></i> Generating...';
    try {
        const response = await fetch(`${API_BASE}/ai/quiz`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                documentIds: [parseInt(documentId)],
                questionCount: parseInt(questionCount.value),
                difficulty: difficulty.value,
                questionTypes: questionTypes
            })
        });
        if (response.ok) {
            const data = await response.json();
            currentQuiz = normalizeQuizPayload(data);
            startQuiz();
        } else {
            throw new Error('Failed to generate quiz');
        }
    } catch (error) {
        console.error('Quiz generation error:', error);
        alert('Failed to generate quiz. Please try again.');
    } finally {
        generateQuizBtn.disabled = false;
        generateQuizBtn.innerHTML = '<i class="bx bx-plus-circle"></i> Generate Quiz';
    }
}
// Preview quiz
function previewQuiz() {
    // This would show a preview of the quiz before starting
    if (window.dashboardUtils && window.dashboardUtils.showNotification) {
        window.dashboardUtils.showNotification('Preview feature coming soon!', 'info');
    } else {
        alert('Preview feature coming soon!');
    }
}
// Start quiz
function startQuiz() {
    currentQuestionIndex = 0;
    quizAnswers = {};
    timeRemaining = currentQuiz.timeLimit * 60; // Convert to seconds
    // Hide creation, show taking interface
    quizCreation.style.display = 'none';
    quizTaking.style.display = 'block';
    quizResults.style.display = 'none';
    // Update quiz info
    currentQuizTitle.textContent = currentQuiz.title;
    updateQuestionDisplay();
    startTimer();
}
// Update question display
function updateQuestionDisplay() {
    if (!currentQuiz || !currentQuiz.questions) return;
    const question = currentQuiz.questions[currentQuestionIndex];
    const totalQuestions = currentQuiz.questions.length;
    // Update progress
    questionProgress.textContent = `Question ${currentQuestionIndex + 1} of ${totalQuestions}`;
    progressFill.style.width = `${((currentQuestionIndex + 1) / totalQuestions) * 100}%`;
    // Update navigation buttons
    prevQuestionBtn.disabled = currentQuestionIndex === 0;
    nextQuestionBtn.style.display = currentQuestionIndex === totalQuestions - 1 ? 'none' : 'block';
    submitQuizBtn.style.display = currentQuestionIndex === totalQuestions - 1 ? 'block' : 'none';
    // Display question
    displayQuestion(question);
}
// Display question
function displayQuestion(question) {
    questionContainer.innerHTML = `
        <div class="question">
            <div class="question-header">
                <div class="question-number">Question ${currentQuestionIndex + 1}</div>
                <div class="question-text">${question.question || question.text || question.prompt || ''}</div>
                <div class="question-type">${question.type}</div>
            </div>
            <div class="answer-options">
                ${generateAnswerOptions(question)}
            </div>
        </div>
    `;
    // Add event listeners to answer options
    addAnswerOptionListeners();
}
// Generate answer options
function normalizeType(type) {
    if (!type) return '';
    switch (type) {
        case 'MCQ':
        case 'multiple_choice':
            return 'multiple_choice';
        case 'TRUE_FALSE':
        case 'true_false':
            return 'true_false';
        default:
            return type;
    }
}

function getOptions(question) {
    // Prefer array fields
    if (Array.isArray(question.options) && question.options.length) return question.options;
    if (Array.isArray(question.choices) && question.choices.length) return question.choices;
    // Common flat fields
    const cand = [];
    const keys = ['optionA','optionB','optionC','optionD','optionE','a','b','c','d','e'];
    keys.forEach(k => {
        if (question[k]) cand.push(question[k]);
    });
    return cand;
}

function generateAnswerOptions(question) {
    const type = normalizeType(question.type);
    if (type === 'multiple_choice') {
        const opts = getOptions(question);
        if (!opts || !opts.length) {
            return `<div class="answer-error">No options provided for this question.</div>`;
        }
        return opts.map((option, index) => `
            <div class="answer-option ${quizAnswers[currentQuestionIndex] === String(index) ? 'selected' : ''}" data-answer="${index}">
                <input type="radio" name="question_${currentQuestionIndex}" value="${index}" 
                       ${quizAnswers[currentQuestionIndex] === String(index) ? 'checked' : ''}>
                <div class="answer-text">${option}</div>
            </div>
        `).join('');
    } else if (type === 'true_false') {
        return `
            <div class="answer-option ${quizAnswers[currentQuestionIndex] === 'true' ? 'selected' : ''}" data-answer="true">
                <input type="radio" name="question_${currentQuestionIndex}" value="true"
                       ${quizAnswers[currentQuestionIndex] === 'true' ? 'checked' : ''}>
                <div class="answer-text">True</div>
            </div>
            <div class="answer-option ${quizAnswers[currentQuestionIndex] === 'false' ? 'selected' : ''}" data-answer="false">
                <input type="radio" name="question_${currentQuestionIndex}" value="false"
                       ${quizAnswers[currentQuestionIndex] === 'false' ? 'checked' : ''}>
                <div class="answer-text">False</div>
            </div>
        `;
    }
    // No fill_blank support
    return '';
}
// Add answer option listeners
function addAnswerOptionListeners() {
    // Scope to current question container
    const scope = questionContainer;
    scope.querySelectorAll('.answer-option').forEach(option => {
        option.addEventListener('click', function() {
            const radio = this.querySelector('input[type="radio"]');
            if (radio) {
                radio.checked = true;
                // Visually mark selected
                scope.querySelectorAll('.answer-option').forEach(o => o.classList.remove('selected'));
                this.classList.add('selected');
                saveAnswer(radio.value);
            }
        });
    });
    // Also listen for direct changes on radio inputs (keyboard navigation, etc.)
    scope.querySelectorAll('.answer-option input[type="radio"]').forEach(radio => {
        radio.addEventListener('change', function() {
            if (this.checked) {
                // Visually mark selected
                scope.querySelectorAll('.answer-option').forEach(o => o.classList.remove('selected'));
                this.closest('.answer-option')?.classList.add('selected');
                saveAnswer(this.value);
            }
        });
    });
}
// Save answer
function saveAnswer(answer) {
    quizAnswers[currentQuestionIndex] = answer;
}
// Previous question
function previousQuestion() {
    if (currentQuestionIndex > 0) {
        currentQuestionIndex--;
        updateQuestionDisplay();
    }
}
// Next question
function nextQuestion() {
    if (currentQuestionIndex < currentQuiz.questions.length - 1) {
        currentQuestionIndex++;
        updateQuestionDisplay();
    }
}
// Start timer
function startTimer() {
    if (timeRemaining <= 0) return;
    updateTimerDisplay();
    
    quizTimer = setInterval(() => {
        timeRemaining--;
        updateTimerDisplay();
        
        if (timeRemaining <= 0) {
            clearInterval(quizTimer);
            submitQuiz();
        }
    }, 1000);
}
// Update timer display
function updateTimerDisplay() {
    const minutes = Math.floor(timeRemaining / 60);
    const seconds = timeRemaining % 60;
    timeRemainingEl.textContent = `Time: ${minutes}:${seconds.toString().padStart(2, '0')}`;
}
// Submit quiz
function submitQuiz() {
    clearInterval(quizTimer);
    
    // Calculate results
    const results = calculateResults();
    
    // Show results
    showResults(results);

    // Persist to recent quizzes and re-render
    try {
        saveRecentQuiz(results);
        renderRecentQuizzes();
    } catch (e) {
        console.warn('Failed to save recent quiz:', e);
    }
}
// Calculate results
function calculateResults() {
    let correct = 0;
    const total = currentQuiz.questions.length;
    const review = [];
    currentQuiz.questions.forEach((question, index) => {
        const userAnswer = quizAnswers[index];
        const isCorrect = checkAnswer(question, userAnswer);
        
        if (isCorrect) correct++;
        review.push({
            question: question,
            userAnswer: userAnswer,
            correctAnswer: question.correctAnswer,
            isCorrect: isCorrect
        });
    });
    const score = Math.round((correct / total) * 100);
    const passed = score >= currentQuiz.passingScore;
    return {
        score: score,
        correct: correct,
        total: total,
        passed: passed,
        review: review,
        timeTaken: currentQuiz.timeLimit * 60 - timeRemaining
    };
}
// Check answer
function checkAnswer(question, userAnswer) {
    const type = normalizeType(question.type);
    if (type === 'multiple_choice') {
        const opts = getOptions(question);
        // correctAnswer might be index or actual option text
        if (typeof question.correctAnswer === 'number') {
            return parseInt(userAnswer) === question.correctAnswer;
        }
        if (typeof question.correctAnswer === 'string') {
            const idx = opts.findIndex(o => String(o).trim().toLowerCase() === question.correctAnswer.trim().toLowerCase());
            return parseInt(userAnswer) === idx;
        }
        return false;
    } else if (type === 'true_false') {
        // correctAnswer might be boolean, 'true'/'false', or 'TRUE'/'FALSE'
        const correct = typeof question.correctAnswer === 'boolean'
            ? String(question.correctAnswer)
            : String(question.correctAnswer).toLowerCase();
        return String(userAnswer).toLowerCase() === correct;
    }
    // No fill_blank support
    return false;
}
// Show results
function showResults(results) {
    // Hide taking, show results
    quizTaking.style.display = 'none';
    quizResults.style.display = 'block';
    // Update score display
    finalScore.textContent = `${results.score}%`;
    scoreText.textContent = results.passed ? 
        'Great job! You passed the quiz.' : 
        'Keep studying! You can do better next time.';
    scoreDetails.textContent = `${results.correct}/${results.total} questions correct`;
    // Update metrics
    correctAnswers.textContent = results.correct;
    incorrectAnswers.textContent = results.total - results.correct;
    timeTaken.textContent = formatTime(results.timeTaken);
    accuracy.textContent = `${results.score}%`;
    // Show question review
    displayQuestionReview(results.review);
}
// Display question review
function displayQuestionReview(review) {
    questionReview.innerHTML = review.map((item, index) => `
        <div class="review-question ${item.isCorrect ? 'correct' : 'incorrect'}">
            <div class="review-question-header">
                <span>Question ${index + 1}</span>
                <span class="${item.isCorrect ? 'correct' : 'incorrect'}">
                    ${item.isCorrect ? '✓ Correct' : '✗ Incorrect'}
                </span>
            </div>
            <div class="review-question-text">${item.question.question}</div>
            <div class="review-answers">
                <div class="review-answer ${item.isCorrect ? 'correct' : 'selected'}">
                    Your Answer: ${formatAnswer(item.question, item.userAnswer)}
                </div>
                ${!item.isCorrect ? `
                    <div class="review-answer correct">
                        Correct Answer: ${formatAnswer(item.question, item.correctAnswer)}
                    </div>
                ` : ''}
            </div>
        </div>
    `).join('');
}
// Format answer for display
function formatAnswer(question, answer) {
    const type = normalizeType(question.type);
    if (type === 'multiple_choice') {
        const opts = getOptions(question);
        return opts[parseInt(answer)] ?? 'No answer';
    } else if (type === 'true_false') {
        return answer === 'true' ? 'True' : 'False';
    } else {
        return 'No answer';
    }
}

// Normalize full quiz payload from backend to enforce only MCQ/TRUE_FALSE types
function normalizeQuizPayload(data) {
    if (!data) return data;
    const copy = { ...data };
    const qs = Array.isArray(data.questions) ? data.questions.slice() : [];
    copy.questions = qs
        .map(q => ({ ...q, type: normalizeType(q.type) }))
        .filter(q => q.type === 'multiple_choice' || q.type === 'true_false')
        .map(q => {
            // unify options array for MCQ if possible
            if (q.type === 'multiple_choice' && !Array.isArray(q.options)) {
                const opts = getOptions(q);
                return { ...q, options: opts };
            }
            return q;
        });
    // ensure timeLimit and passingScore have sensible defaults
    if (typeof copy.timeLimit !== 'number') copy.timeLimit = parseInt(timeLimit.value || '10') || 10;
    if (typeof copy.passingScore !== 'number') copy.passingScore = parseInt(passingScore.value || '70') || 70;
    // carry title/topic if missing
    if (!copy.title) copy.title = quizTitle.value || 'Generated Quiz';
    if (!copy.topic) copy.topic = quizTopic.value || '';
    // record documentIds for recent-quizzes card
    if (!Array.isArray(copy.documentIds)) copy.documentIds = [parseInt(quizDocument.value)];
    return copy;
}
// Exit quiz
function exitQuiz() {
    if (confirm('Are you sure you want to exit? Your progress will be lost.')) {
        clearInterval(quizTimer);
        resetQuiz();
    }
}
// Retake quiz
function retakeQuiz() {
    resetQuiz();
    startQuiz();
}
// Create new quiz
function createNewQuiz() {
    resetQuiz();
}
// Download results
function downloadResults() {
    // This would generate and download a PDF/Excel report
    showNotification('Download feature coming soon!', 'info');
}
// Reset quiz
function resetQuiz() {
    currentQuiz = null;
    currentQuestionIndex = 0;
    quizAnswers = {};
    clearInterval(quizTimer);
    
    // Show creation interface
    quizCreation.style.display = 'block';
    quizTaking.style.display = 'none';
    quizResults.style.display = 'none';
    
    // Reset form
    quizTitle.value = '';
    quizTopic.value = '';
    document.querySelectorAll('input[type="checkbox"]').forEach(cb => cb.checked = false);
    questionCount.value = '10';
    difficulty.value = 'medium';
    timeLimit.value = '10';
    passingScore.value = '70';
}
// Recent quizzes (localStorage-based)
const RECENT_QUIZZES_KEY = 'recentQuizzes';

function getRecentQuizzes() {
    try {
        const raw = localStorage.getItem(RECENT_QUIZZES_KEY);
        return raw ? JSON.parse(raw) : [];
    } catch (e) {
        return [];
    }
}

function saveRecentQuiz(results) {
    if (!currentQuiz) return;
    const recent = getRecentQuizzes();
    const entry = {
        id: Date.now(),
        title: currentQuiz.title || 'Untitled Quiz',
        topic: (typeof currentQuiz.topic === 'string' ? currentQuiz.topic : ''),
        documentId: (Array.isArray(currentQuiz.documentIds) ? currentQuiz.documentIds[0] : null),
        questionCount: currentQuiz.questions ? currentQuiz.questions.length : 0,
        score: results.score,
        correct: results.correct,
        total: results.total,
        passed: results.passed,
        timeTaken: results.timeTaken,
        timestamp: new Date().toISOString()
    };
    recent.unshift(entry);
    // keep only latest 10
    const trimmed = recent.slice(0, 10);
    localStorage.setItem(RECENT_QUIZZES_KEY, JSON.stringify(trimmed));
}

function renderRecentQuizzes() {
    if (!quizzesGrid) return;
    const recent = getRecentQuizzes();
    if (!recent.length) {
        quizzesGrid.innerHTML = '<div class="empty-state">No recent quizzes yet. Generate a quiz to see it here.</div>';
        return;
    }
    quizzesGrid.innerHTML = recent.map(q => `
        <div class="quiz-card ${q.passed ? 'passed' : 'failed'}">
            <div class="quiz-card-header">
                <h4>${escapeHtml(q.title)}</h4>
                <span class="chip ${q.passed ? 'chip-success' : 'chip-warning'}">${q.score}%</span>
            </div>
            <div class="quiz-card-body">
                <div class="meta"><i class='bx bx-time'></i> ${formatTime(q.timeTaken)}</div>
                <div class="meta"><i class='bx bx-help-circle'></i> ${q.correct}/${q.total} correct</div>
                ${q.topic ? `<div class="meta"><i class='bx bx-book-content'></i> ${escapeHtml(q.topic)}</div>` : ''}
                <div class="meta small">${formatDate(q.timestamp)}</div>
            </div>
        </div>
    `).join('');
}

function formatDate(iso) {
    try {
        const d = new Date(iso);
        return d.toLocaleString();
    } catch { return ''; }
}

function escapeHtml(str) {
    if (typeof str !== 'string') return '';
    return str.replace(/&/g, '&amp;')
              .replace(/</g, '&lt;')
              .replace(/>/g, '&gt;')
              .replace(/"/g, '&quot;')
              .replace(/'/g, '&#039;');
}
// Recent quizzes feature removed (no backend support for /api/quizzes).
// Take quiz (for existing quizzes)
function takeQuiz(quizId) {
    // This would load an existing quiz
    showNotification('Loading quiz...', 'info');
}
// View quiz results
function viewQuizResults(quizId) {
    // This would show results for a specific quiz
    showNotification('Loading results...', 'info');
}
// Utility functions
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}
function formatTime(seconds) {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
}
// Notification system
function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.innerHTML = `
        <i class='bx ${getNotificationIcon(type)}'></i>
        <span>${message}</span>
        
    `;
    
    document.body.appendChild(notification);
    
    // Auto remove after 5 seconds
    setTimeout(() => {
        if (notification.parentElement) {
            notification.remove();
        }
    }, 5000);
}
function getNotificationIcon(type) {
    switch (type) {
        case 'success': return 'bx-check-circle';
        case 'error': return 'bx-error-circle';
        case 'warning': return 'bx-warning';
        default: return 'bx-info-circle';
    }
}
