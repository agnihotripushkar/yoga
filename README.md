# YogSadhna Backend API

A secure Spring Boot backend service for the YogSadhna application, providing comprehensive features for yoga practice, wellness tracking, and personalized diet plans. This service supports OAuth authentication (Google/Apple), JWT token management, and extensive user profile capabilities.

## 🚀 Features

### Core Features
- **OAuth Authentication**: Google and Apple Sign-In integration
- **JWT Token Management**: Secure access and refresh token handling with UUID support
- **Rate Limiting**: IP and User-based rate limiting for API protection
- **User Profile Management**: Complete user profile CRUD operations with extended stats (height, weight, level)
- **Security**: Spring Security integration with custom authentication and input sanitization

### Yoga Features
- **Class Management**: Browse and search yoga classes with filters (difficulty, duration)
- **Session Tracking**: Record completed sessions with calories burned and duration
- **Progress Analytics**: Weekly and monthly progress summaries
- **Favorites**: Bookmark favorite classes

### New Features (v2.0)
- **Wellness Tracking**: Log daily health metrics (weight, heart rate, hydration, sleep, mood)
- **AI Diet Plans**: Generate personalized 7-day meal plans based on goals (Weight Loss, Muscle Build) and dietary preferences
- **Extended Profile**: Detailed user statistics including total practice minutes and level progression

## 🛠️ Tech Stack

- **Language**: Kotlin 1.9.22
- **Framework**: Spring Boot 3.2.1
- **Security**: Spring Security + JWT
- **Database**: PostgreSQL (Production) / H2 (Development)
- **AI Integration**: Google Gemini API (for Diet Plans)
- **Build Tool**: Gradle
- **Java Version**: 17+

## 📋 Prerequisites

- Java 17 or higher
- Gradle 7.0+ (or use included wrapper)
- Google OAuth 2.0 credentials
- Apple Developer account (for Apple Sign-In)
- Google Gemini API Key (for AI features)

## 🔧 Installation & Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd yoga
   ```

2. **Configure Environment Variables**
   Create `application.properties` or set environment variables:
   ```properties
   # OAuth
   SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID=your_google_client_id
   SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_APPLE_CLIENT_ID=your_apple_client_id
   
   # JWT
   JWT_SECRET=your_jwt_secret_key_must_be_long_enough
   JWT_EXPIRATION_MS=86400000
   
   # Database
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/yoga_db
   SPRING_DATASOURCE_USERNAME=postgres
   SPRING_DATASOURCE_PASSWORD=password
   
   # AI
   GEMINI_API_KEY=your_gemini_api_key
   ```

3. **Build the project**
   ```bash
   ./gradlew build
   ```

4. **Run the application**
   ```bash
   ./gradlew bootRun
   ```

The application will start on `http://localhost:8080`

## 📚 API Overview

The application comes with fully interactive Swagger/OpenAPI documentation.

**Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)  
**OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

For static detailed documentation, see [API_DOCUMENTATION.md](./API_DOCUMENTATION.md).

### Authentication
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth/google/login` | POST | Google OAuth login |
| `/api/auth/profile` | GET | Get user profile (UUID based) |

### Yoga Classes
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/classes` | GET | List classes with filters |
| `/api/classes/{id}` | GET | Get class details (UUID) |
| `/api/classes/favorites` | GET | Get user favorites |

### Progress & Wellness
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/progress/session` | POST | Record a yoga session |
| `/api/progress/summary` | GET | Get user progress stats |
| `/api/wellness/log` | POST | Log daily health metrics |
| `/api/wellness/history` | GET | Get health history |

### AI Diet
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/diet/generate` | POST | Generate AI diet plan |
| `/api/diet/active` | GET | Get current diet plan |

## 🏗️ Project Structure

```
src/main/kotlin/com/devpush/yoga/
├── features/
│   ├── auth/           # Authentication & User features
│   ├── classes/        # Yoga Class management
│   ├── progress/       # Session tracking & stats
│   ├── wellness/       # Health tracking features
│   └── diet/           # AI Diet features
├── entity/             # JPA entities (UUID based)
├── repository/         # Data access layer
├── service/            # Business logic
└── config/             # Configuration classes
```

## 🧪 Testing

Run tests with:
```bash
./gradlew test
```

## 🔒 Security Features

- **UUIDs**: All primary keys migrated to UUIDs to prevent enumeration attacks.
- **Role-Based Access**: Granular permissions.
- **Input Sanitization**: Protection against injection attacks.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
