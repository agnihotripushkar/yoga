# Yoga App API Documentation

## Overview

The Yoga App API provides a comprehensive backend system for a yoga application with OAuth authentication, profile management, progress tracking, and yoga class management features.

## Base URL

- **Development**: `http://localhost:8080`
- **Production**: `https://api.yogaapp.com`

## Interactive Documentation

Once the application is running, you can access the interactive Swagger UI documentation at:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/api-docs`

## Authentication

The API uses JWT (JSON Web Tokens) for authentication. Most endpoints require a valid access token in the Authorization header.

### Header Format
```
Authorization: Bearer <your-jwt-token>
```

### Getting Started

1. **Sign in with OAuth**: Use Google or Apple Sign-In to get JWT tokens
2. **Use Access Token**: Include the access token in subsequent API calls
3. **Refresh Tokens**: Use the refresh token to get new access tokens when they expire

## API Endpoints

### Authentication (`/api/auth`)

#### POST `/api/auth/google/login`
Authenticate with Google ID token.

**Request Body:**
```json
{
  "idToken": "google-id-token-here"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "name": "John Doe",
    "profilePicture": null,
    "fitnessLevel": "BEGINNER"
  }
}
```

#### POST `/api/auth/apple/login`
Authenticate with Apple ID token.

#### POST `/api/auth/refresh`
Refresh access token using refresh token.

#### POST `/api/auth/logout`
Invalidate refresh token and logout.

#### GET `/api/auth/profile`
Get authenticated user's profile information.

### Profile Management (`/api/profile`)

#### PUT `/api/profile`
Update user profile information.

**Request Body:**
```json
{
  "name": "John Doe",
  "bio": "Yoga enthusiast and beginner",
  "fitnessLevel": "INTERMEDIATE"
}
```

#### POST `/api/profile/picture`
Upload profile picture (multipart/form-data).

**Form Data:**
- `file`: Image file (JPEG, PNG, WebP, max 5MB)

#### DELETE `/api/profile/picture`
Remove current profile picture.

### Progress Tracking (`/api/progress`)

#### POST `/api/progress/session`
Record a completed yoga session.

**Request Body:**
```json
{
  "classId": 1,
  "durationMinutes": 45,
  "notes": "Great session!"
}
```

#### GET `/api/progress/summary`
Get overall progress statistics.

**Response:**
```json
{
  "totalSessions": 25,
  "totalDurationMinutes": 1125,
  "totalCaloriesBurned": 4500,
  "averageSessionDuration": 45,
  "currentStreak": 7,
  "longestStreak": 12
}
```

#### GET `/api/progress/weekly?weekOffset=0`
Get weekly progress data.

**Query Parameters:**
- `weekOffset`: Week offset (0 = current week, 1 = last week, max 52)

#### GET `/api/progress/monthly?monthOffset=0`
Get monthly progress data.

**Query Parameters:**
- `monthOffset`: Month offset (0 = current month, 1 = last month, max 24)

### Yoga Classes (`/api/classes`)

#### GET `/api/classes`
Get paginated list of yoga classes with optional filtering.

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 20, max: 100)
- `difficultyLevel`: Filter by difficulty (BEGINNER, INTERMEDIATE, ADVANCED)
- `minDuration`: Minimum duration in minutes
- `maxDuration`: Maximum duration in minutes
- `instructor`: Filter by instructor name

#### GET `/api/classes/{classId}`
Get detailed information about a specific yoga class.

#### GET `/api/classes/search`
Search yoga classes by title, description, or instructor.

**Query Parameters:**
- `query`: Search text
- `page`, `size`: Pagination parameters
- Additional filter parameters (same as `/api/classes`)

#### POST `/api/classes/{classId}/favorite`
Add a class to user's favorites.

#### DELETE `/api/classes/{classId}/favorite`
Remove a class from user's favorites.

#### GET `/api/classes/favorites`
Get user's favorite classes.

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 20, max: 100)

## Rate Limiting

The API implements rate limiting to ensure fair usage:

- **Authentication endpoints**: 10 requests/minute
- **File uploads**: 5 requests/minute
- **Profile updates**: 10 requests/minute
- **Progress queries**: 60 requests/minute
- **Class searches**: 100 requests/minute

When rate limits are exceeded, the API returns a `429 Too Many Requests` status code.

## Error Handling

The API uses standard HTTP status codes and returns structured error responses:

```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable error message",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

### Common Status Codes

- `200 OK`: Successful GET requests
- `201 Created`: Successful POST requests that create resources
- `204 No Content`: Successful DELETE requests
- `400 Bad Request`: Invalid request data or parameters
- `401 Unauthorized`: Missing or invalid authentication
- `403 Forbidden`: Access denied
- `404 Not Found`: Resource not found
- `409 Conflict`: Resource already exists (e.g., duplicate favorite)
- `413 Payload Too Large`: File upload size exceeded
- `415 Unsupported Media Type`: Invalid file type
- `429 Too Many Requests`: Rate limit exceeded
- `500 Internal Server Error`: Server error

## Data Models

### User Profile
```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "John Doe",
  "bio": "Yoga enthusiast",
  "fitnessLevel": "INTERMEDIATE",
  "profilePicture": "https://example.com/profile.jpg"
}
```

### Yoga Class
```json
{
  "id": 1,
  "title": "Morning Flow",
  "description": "Energizing morning yoga flow",
  "durationMinutes": 30,
  "difficultyLevel": "BEGINNER",
  "instructor": "Sarah Johnson",
  "videoUrl": "https://example.com/video.mp4",
  "thumbnailUrl": "https://example.com/thumbnail.jpg",
  "isFavorite": false
}
```

### Yoga Session
```json
{
  "id": 123,
  "durationMinutes": 45,
  "caloriesBurned": 180,
  "completedAt": "2024-01-15T10:30:00Z",
  "yogaClass": {
    "id": 1,
    "title": "Morning Flow"
  },
  "notes": "Great session!"
}
```

## Sample Requests

### cURL Examples

#### Login with Google
```bash
curl -X POST http://localhost:8080/api/auth/google/login \
  -H "Content-Type: application/json" \
  -d '{"idToken": "your-google-id-token"}'
```

#### Get Classes
```bash
curl -X GET "http://localhost:8080/api/classes?page=0&size=10&difficultyLevel=BEGINNER" \
  -H "Authorization: Bearer your-access-token"
```

#### Record Session
```bash
curl -X POST http://localhost:8080/api/progress/session \
  -H "Authorization: Bearer your-access-token" \
  -H "Content-Type: application/json" \
  -d '{"classId": 1, "durationMinutes": 30}'
```

#### Upload Profile Picture
```bash
curl -X POST http://localhost:8080/api/profile/picture \
  -H "Authorization: Bearer your-access-token" \
  -F "file=@profile.jpg"
```

## Development Setup

1. **Start the application**: `./gradlew bootRun`
2. **Access Swagger UI**: `http://localhost:8080/swagger-ui.html`
3. **View API docs**: `http://localhost:8080/api-docs`

## Production Configuration

For production deployment, ensure the following environment variables are set:

- `DATABASE_URL`: PostgreSQL connection URL
- `DATABASE_USERNAME`: Database username
- `DATABASE_PASSWORD`: Database password
- `JWT_SECRET`: Strong JWT secret key
- `GOOGLE_CLIENT_ID`: Google OAuth client ID
- `APPLE_TEAM_ID`, `APPLE_KEY_ID`, `APPLE_CLIENT_ID`, `APPLE_PRIVATE_KEY`: Apple OAuth configuration

## Support

For API support and questions, contact the development team at dev@yogaapp.com.