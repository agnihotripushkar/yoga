# Yoga Authentication API

A secure Spring Boot backend service providing OAuth authentication for a Yoga application. This service supports Google and Apple Sign-In with JWT token management, rate limiting, and comprehensive user profile management.

## 🚀 Features

- **OAuth Authentication**: Google and Apple Sign-In integration
- **JWT Token Management**: Secure access and refresh token handling
- **Rate Limiting**: IP-based authentication rate limiting
- **User Profile Management**: Complete user profile CRUD operations
- **Security**: Spring Security integration with custom authentication
- **Validation**: Request validation with detailed error messages
- **Logging**: Comprehensive logging for monitoring and debugging

## 🛠️ Tech Stack

- **Language**: Kotlin
- **Framework**: Spring Boot 3.5.6
- **Security**: Spring Security + JWT
- **Database**: H2 (development), JPA/Hibernate
- **Build Tool**: Gradle
- **Java Version**: 21

## 📋 Prerequisites

- Java 21 or higher
- Gradle 7.0+ (or use included wrapper)
- Google OAuth 2.0 credentials (for Google Sign-In)
- Apple Developer account and Sign in with Apple setup

## 🔧 Installation & Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd yoga
   ```

2. **Configure OAuth credentials**
   Create `application.yml` or `application.properties` with your OAuth settings:
   ```yaml
   # Add your Google and Apple OAuth configurations here
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

## 📚 API Documentation

For detailed API documentation with request/response examples, see [API_DOCUMENTATION.md](./API_DOCUMENTATION.md)

### Quick API Overview

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth/google/login` | POST | Google OAuth login |
| `/api/auth/apple/login` | POST | Apple OAuth login |
| `/api/auth/refresh` | POST | Refresh JWT tokens |
| `/api/auth/logout` | POST | Logout and revoke tokens |
| `/api/auth/profile` | GET | Get user profile |

## 🔐 Authentication Flow

1. **Login**: Client sends OAuth ID token to `/google/login` or `/apple/login`
2. **Token Validation**: Server validates the OAuth token with provider
3. **JWT Generation**: Server generates access and refresh tokens
4. **API Access**: Client uses access token for authenticated requests
5. **Token Refresh**: Client uses refresh token to get new access tokens
6. **Logout**: Client revokes refresh token

## 🏗️ Project Structure

```
src/main/kotlin/com/devpush/yoga/
├── controller/          # REST controllers
│   └── AuthController.kt
├── dto/                # Data Transfer Objects
│   ├── AuthResponse.kt
│   ├── GoogleLoginRequest.kt
│   ├── AppleLoginRequest.kt
│   ├── RefreshTokenRequest.kt
│   └── UserProfile.kt
├── entity/             # JPA entities
│   └── OAuthProvider.kt
├── service/            # Business logic services
├── util/              # Utility classes
└── config/            # Configuration classes
```

## 🧪 Testing

Run tests with:
```bash
./gradlew test
```

## 🔒 Security Features

- **Rate Limiting**: Prevents brute force attacks
- **JWT Validation**: Secure token-based authentication
- **OAuth Integration**: Leverages trusted identity providers
- **Input Validation**: Comprehensive request validation
- **Error Handling**: Secure error responses without sensitive data leakage

## 📊 Monitoring & Logging

The application includes comprehensive logging for:
- Authentication attempts and results
- Token operations (generation, refresh, revocation)
- Rate limiting events
- Error conditions and exceptions

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

For support and questions, please open an issue in the GitHub repository.
