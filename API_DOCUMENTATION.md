# Yoga Authentication API Documentation

## Base URL
```
http://localhost:8080/api/auth
```

## Authentication
Most endpoints require a Bearer token in the Authorization header:
```
Authorization: Bearer <access_token>
```

## Rate Limiting
Authentication endpoints (`/google/login`, `/apple/login`) are rate-limited by IP address to prevent abuse.

---

## Endpoints

### 1. Google OAuth Login

Authenticate users using Google Sign-In ID tokens.

**Endpoint:** `POST /api/auth/google/login`

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "idToken": "string (required)"
}
```

**Success Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "def502004a8b7c9d1e2f3a4b5c6d7e8f9a0b1c2d...",
  "expiresIn": 3600,
  "user": {
    "id": 123,
    "email": "user@gmail.com",
    "name": "John Doe",
    "profilePicture": "https://lh3.googleusercontent.com/a/...",
    "provider": "GOOGLE"
  }
}
```

**Error Responses:**
- `400 Bad Request`: Invalid or missing ID token
- `401 Unauthorized`: Invalid Google ID token
- `429 Too Many Requests`: Rate limit exceeded

**Example cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/google/login \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjE2NzAyN..."
  }'
```

---

### 2. Apple OAuth Login

Authenticate users using Apple Sign-In ID tokens.

**Endpoint:** `POST /api/auth/apple/login`

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "idToken": "string (required)"
}
```

**Success Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "def502004a8b7c9d1e2f3a4b5c6d7e8f9a0b1c2d...",
  "expiresIn": 3600,
  "user": {
    "id": 124,
    "email": "user@privaterelay.appleid.com",
    "name": "Jane Smith",
    "profilePicture": null,
    "provider": "APPLE"
  }
}
```

**Error Responses:**
- `400 Bad Request`: Invalid or missing ID token
- `401 Unauthorized`: Invalid Apple ID token
- `429 Too Many Requests`: Rate limit exceeded

**Example cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/apple/login \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "eyJraWQiOiJmaDZCczhDIiwiYWxnIjoiUlMyNTYifQ..."
  }'
```

---

### 3. Refresh Token

Generate new access and refresh tokens using a valid refresh token.

**Endpoint:** `POST /api/auth/refresh`

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "refreshToken": "string (required)"
}
```

**Success Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "abc123004a8b7c9d1e2f3a4b5c6d7e8f9a0b1c2d...",
  "expiresIn": 3600,
  "user": {
    "id": 123,
    "email": "user@gmail.com",
    "name": "John Doe",
    "profilePicture": "https://lh3.googleusercontent.com/a/...",
    "provider": "GOOGLE"
  }
}
```

**Error Responses:**
- `400 Bad Request`: Invalid or missing refresh token
- `401 Unauthorized`: Expired or revoked refresh token

**Example cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "def502004a8b7c9d1e2f3a4b5c6d7e8f9a0b1c2d..."
  }'
```

---

### 4. Logout

Revoke a refresh token to log out the user.

**Endpoint:** `POST /api/auth/logout`

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "refreshToken": "string (required)"
}
```

**Success Response (204 No Content):**
```
(Empty response body)
```

**Error Responses:**
- `400 Bad Request`: Invalid or missing refresh token
- `401 Unauthorized`: Token already revoked or invalid

**Example cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "def502004a8b7c9d1e2f3a4b5c6d7e8f9a0b1c2d..."
  }'
```

---

### 5. Get User Profile

Retrieve the authenticated user's profile information.

**Endpoint:** `GET /api/auth/profile`

**Headers:**
```
Authorization: Bearer <access_token>
```

**Success Response (200 OK):**
```json
{
  "id": 123,
  "email": "user@gmail.com",
  "name": "John Doe",
  "profilePicture": "https://lh3.googleusercontent.com/a/...",
  "provider": "GOOGLE"
}
```

**Error Responses:**
- `401 Unauthorized`: Missing, invalid, or expired access token
- `404 Not Found`: User not found

**Example cURL:**
```bash
curl -X GET http://localhost:8080/api/auth/profile \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## Data Models

### AuthResponse
```json
{
  "accessToken": "string",      // JWT access token
  "refreshToken": "string",     // JWT refresh token
  "expiresIn": "number",        // Token expiration time in seconds
  "user": "UserProfile"         // User profile object
}
```

### UserProfile
```json
{
  "id": "number",               // Unique user ID
  "email": "string",            // User's email address
  "name": "string|null",        // User's display name (optional)
  "profilePicture": "string|null", // Profile picture URL (optional)
  "provider": "string"          // OAuth provider (GOOGLE or APPLE)
}
```

### GoogleLoginRequest
```json
{
  "idToken": "string"           // Google ID token (required)
}
```

### AppleLoginRequest
```json
{
  "idToken": "string"           // Apple ID token (required)
}
```

### RefreshTokenRequest
```json
{
  "refreshToken": "string"      // Refresh token (required)
}
```

---

## Error Handling

All error responses follow this format:

```json
{
  "timestamp": "2024-01-15T10:30:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Detailed error message",
  "path": "/api/auth/google/login"
}
```

### Common Error Codes

- **400 Bad Request**: Invalid request format or missing required fields
- **401 Unauthorized**: Authentication failed or token invalid/expired
- **404 Not Found**: Resource not found
- **429 Too Many Requests**: Rate limit exceeded
- **500 Internal Server Error**: Server error

---

## Authentication Flow

### Initial Login Flow
1. User initiates OAuth login (Google/Apple) in client app
2. Client receives ID token from OAuth provider
3. Client sends ID token to `/google/login` or `/apple/login`
4. Server validates ID token with OAuth provider
5. Server creates/updates user record
6. Server generates JWT access and refresh tokens
7. Server returns tokens and user profile

### Subsequent API Calls
1. Client includes access token in Authorization header
2. Server validates access token
3. Server processes request and returns response

### Token Refresh Flow
1. When access token expires, client calls `/refresh` with refresh token
2. Server validates refresh token
3. Server generates new access and refresh tokens
4. Client updates stored tokens

### Logout Flow
1. Client calls `/logout` with refresh token
2. Server revokes the refresh token
3. Client clears stored tokens

---

## Rate Limiting

Authentication endpoints have the following rate limits:
- **Google/Apple Login**: Limited by IP address
- **Rate limit exceeded**: Returns 429 status code

---

## Security Considerations

1. **HTTPS Only**: Always use HTTPS in production
2. **Token Storage**: Store tokens securely on client side
3. **Token Expiration**: Access tokens have short expiration times
4. **Refresh Token Security**: Refresh tokens should be stored securely
5. **Rate Limiting**: Prevents brute force attacks
6. **Input Validation**: All inputs are validated server-side

---

## Testing with Postman

Import this collection to test the API:

1. Create a new Postman collection
2. Add environment variables:
   - `base_url`: `http://localhost:8080`
   - `access_token`: (set after login)
   - `refresh_token`: (set after login)

3. Test the endpoints in this order:
   - Google/Apple Login → Get tokens
   - Get Profile → Test authentication
   - Refresh Token → Test token refresh
   - Logout → Test token revocation