# SmartStudy - AI-Powered Learning Assistant

SmartStudy is an intelligent learning platform that helps students study more effectively using AI. It allows users to upload study materials, generate quizzes, search content, and get AI-powered assistance.

## 🚀 Features

### Core Features

#### Document Management
- **Multi-format Support**: Upload and organize PDF, DOCX, PPT, and TXT files
- **Smart Organization**: Automatic categorization and tagging of study materials
- **Version Control**: Keep track of document versions and updates

#### AI-Powered Learning Tools
- **Smart Flashcards**: Automatically generate flashcards from your notes
- **Key Concepts Extraction**: AI identifies and highlights important concepts
- **Spaced Repetition**: Optimized review schedule for better retention
- **Quiz Generation**: Create custom quizzes with various question types
- **Quiz Review System**: Detailed analytics on past quiz attempts

#### Smart Search & Assistance
- **Semantic Search**: Find information using natural language queries
- **AI Study Assistant**: Get explanations and detailed answers
- **Concept Mapping**: Visualize relationships between key concepts

#### Analytics & Progress
- **Performance Dashboard**: Track scores, study time, and progress
- **Knowledge Gaps**: Identify weak areas that need more focus
- **Study Trends**: Monitor improvement over time with visual analytics
- **Achievement System**: Earn badges and milestones for learning goals

## 🛠️ Tech Stack

### Backend
- **Java 17** - Core programming language
- **Spring Boot 3.2.0** - Application framework
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Database operations
- **MySQL** - Primary database
- **Apache Lucene** - Full-text search
- **Google Gemini API** - AI-powered content generation
- **Maven** - Dependency management

### Frontend
- **HTML5, CSS3, JavaScript** - Core web technologies
- **Vanilla JS** - No frontend framework dependency
- **Chart.js** - Data visualization for analytics
- **Responsive Design** - Works on all devices

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- MySQL 8.0+
- Maven 3.6+
- Node.js & npm (for frontend development)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/smartstudy-app.git
   cd smartstudy-app
   ```

2. **Set up the database**
   ```sql
   CREATE DATABASE smartstudy_db;
   ```

3. **Configure the application**
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```
   - Update database credentials and other settings
   - Add your Gemini API key for AI features

4. **Build and run the application**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

5. **Access the application**
   - Open `http://localhost:8080` in your browser

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/smartstudy/
│   │   ├── config/       # Configuration classes
│   │   ├── controller/   # REST controllers
│   │   ├── model/        # Entity classes
│   │   ├── repository/   # Data access layer
│   │   ├── service/      # Business logic
│   │   └── SmartStudyApplication.java
│   └── resources/
│       ├── static/       # Frontend assets (JS, CSS, images)
│       ├── templates/    # HTML templates
│       └── application.properties
```

## 🔒 Authentication & User Management

SmartStudy features a robust authentication system:
- **Secure Session Management**
- **Role-based Access Control** (Admin/User roles)
- **Password Encryption** using BCrypt
- **Session Timeout** after 30 minutes of inactivity

### Default Admin Credentials
- **Username**: admin
- **Password**: changeit

> **Security Note**: Change the default admin password after first login.

## 📊 Analytics & Monitoring

### Learning Analytics Dashboard
- **Performance Overview**
  - Quiz scores and completion rates
  - Time spent studying each topic
  - Concept mastery levels
  - Progress towards learning goals

- **Flashcard Analytics**
  - Recall accuracy rates
  - Spaced repetition effectiveness
  - Most challenging concepts
  - Study session patterns

- **Quiz Performance**
  - Score trends over time
  - Question-level analysis
  - Time management metrics
  - Comparison with peer performance

### System Monitoring
- **User Activity**
  - Document upload and access patterns
  - Quiz and flashcard interactions
  - Search query analytics
  - Active user sessions

- **Performance Metrics**
  - System response times
  - API usage statistics
  - Resource utilization
  - Error rates and diagnostics

### Data Export
- Export analytics reports in PDF/CSV
- Share progress with instructors
- Print study summaries

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.


### Search Engine
- **Apache Lucene** - Full-text search and indexing

### AI Services
- **Google Gemini API** - Summarization, quiz generation, and concept extraction
- **Spaced Repetition Algorithm** - Optimizes flashcard review schedule
- **Natural Language Processing** - For key concept extraction and question generation
- **Learning Analytics Engine** - Tracks and predicts learning progress

### Frontend
- **HTML5/CSS3/JavaScript**
- **Responsive Design**

## 📋 Prerequisites

Before running this application, make sure you have:

- **Java 17** or higher
- **Maven 3.6** or higher
- **MySQL 8.0** or higher
- **Google Gemini API Key** (free tier available)

## 🚀 Quick Start

### 1. Database Setup

```sql
-- Create database
CREATE DATABASE smartstudy CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Use the database
USE smartstudy;
```

### 2. Configuration

Update `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/smartstudy?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=your_username
spring.datasource.password=your_password

# Gemini API Configuration
gemini.api.key=your-gemini-api-key-here
```

### 3. Build and Run

```bash
# Clone the repository
git clone <repository-url>
cd smart-study-app

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## 📁 Project Structure

```
smart-study-app/
├── src/main/java/com/smartstudy/
│   ├── SmartStudyApplication.java          # Main application class
│   ├── controller/                         # REST controllers
│   │   ├── DocumentController.java
│   │   ├── SearchController.java
│   │   └── QuizController.java
│   ├── service/                           # Business logic
│   │   ├── DocumentService.java
│   │   ├── SearchService.java
│   │   ├── QuizService.java
│   │   └── GeminiService.java
│   ├── model/                             # Entity classes
│   │   ├── Document.java
│   │   ├── DocumentContent.java
│   │   └── Quiz.java
│   ├── repository/                        # Data access layer
│   │   ├── DocumentRepository.java
│   │   ├── DocumentContentRepository.java
│   │   └── QuizRepository.java
│   ├── dto/                               # Data transfer objects
│   │   ├── DocumentUploadResponse.java
│   │   ├── SearchRequest.java
│   │   └── SearchResponse.java
│   └── config/                            # Configuration classes
│       └── LuceneConfig.java
├── src/main/resources/
│   ├── application.properties             # Application configuration
│   └── static/                            # Static files (HTML, CSS, JS)
├── pom.xml                                # Maven dependencies
└── README.md
```

## 🔧 API Endpoints

### Document Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/documents/upload` | Upload a ZIP file |
| GET | `/api/documents` | Get all documents |
| GET | `/api/documents/{id}` | Get document by ID |
| GET | `/api/documents/status/{status}` | Get documents by status |
| DELETE | `/api/documents/{id}` | Delete document |
| GET | `/api/documents/stats` | Get document statistics |

### Search & Q&A

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/search` | Search documents |
| POST | `/api/search/summarize` | Get summarization |

### Quiz Generation

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/quiz/generate` | Generate quiz |
| GET | `/api/quiz` | Get all quizzes |
| GET | `/api/quiz/{id}` | Get quiz by ID |

## 🎯 Usage Examples



## 🔑 Getting Gemini API Key

1. Go to [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Sign in with your Google account
3. Click "Create API Key"
4. Copy the API key and add it to `application.properties`

**Free Tier Limits:**
- 15 requests per minute
- 1M characters per month
- No credit card required


## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.



## 🔮 Roadmap

- [ ] Real-time document processing status
- [ ] Advanced search filters
- [ ] Export quiz results
- [ ] Mobile application
- [ ] Integration with learning management systems
- [ ] Advanced analytics and insights

---
## Images

<img width="1919" height="969" alt="image" src="https://github.com/user-attachments/assets/c80bde1d-f76d-455b-afd9-19ce28651bc4" />
<img width="1919" height="975" alt="image" src="https://github.com/user-attachments/assets/21d58151-5341-4f70-a35d-e795d760e2c8" />
<img width="1919" height="971" alt="image" src="https://github.com/user-attachments/assets/54007e16-4d21-4131-9b71-6c420486aa84" />
<img width="1919" height="968" alt="image" src="https://github.com/user-attachments/assets/838f8950-b44f-478a-a5d3-a8e59feb3ffa" />
<img width="1912" height="971" alt="image" src="https://github.com/user-attachments/assets/983a07a6-05a2-4744-8808-5061f3f159a9" />
<img width="1919" height="966" alt="image" src="https://github.com/user-attachments/assets/5d940de3-e913-4d93-b445-41dec04f2396" />
<img width="1917" height="972" alt="image" src="https://github.com/user-attachments/assets/04160248-3c93-4392-8826-b6eb08e76eb2" />
<img width="1876" height="946" alt="image" src="https://github.com/user-attachments/assets/2ed8a578-abf2-4b47-bbc1-f5911029051c" />
<img width="1917" height="966" alt="image" src="https://github.com/user-attachments/assets/517c128c-20e2-4845-bc97-7fbc503d6a03" />










