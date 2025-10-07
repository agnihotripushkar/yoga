package com.devpush.yoga.controller

import com.devpush.yoga.dto.ErrorResponse
import com.devpush.yoga.service.AuthenticationException
import com.devpush.yoga.service.TokenRefreshException
import com.devpush.yoga.service.LogoutException
import com.devpush.yoga.service.GoogleTokenValidationException
import com.devpush.yoga.service.AppleTokenValidationException
import com.devpush.yoga.exception.RateLimitExceededException
import com.devpush.yoga.exception.FileUploadException
import com.devpush.yoga.util.SecurityLogger
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.SignatureException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException as SpringAuthenticationException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.time.LocalDateTime
import java.util.concurrent.TimeoutException

@RestControllerAdvice
class GlobalExceptionHandler {
    
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    
    /**
     * Handle authentication exceptions (401 Unauthorized)
     */
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ResponseEntity<ErrorResponse> {
        SecurityLogger.logSecurityError("AUTHENTICATION_ERROR", ex.message ?: "Authentication failed")
        
        val errorResponse = ErrorResponse(
            error = "AUTHENTICATION_FAILED",
            message = ex.message ?: "Authentication failed",
            status = HttpStatus.UNAUTHORIZED.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }
    
    /**
     * Handle token refresh exceptions (401 Unauthorized)
     */
    @ExceptionHandler(TokenRefreshException::class)
    fun handleTokenRefreshException(ex: TokenRefreshException): ResponseEntity<ErrorResponse> {
        logger.error("Token refresh error: {}", ex.message)
        
        val errorResponse = ErrorResponse(
            error = "TOKEN_REFRESH_FAILED",
            message = ex.message ?: "Token refresh failed",
            status = HttpStatus.UNAUTHORIZED.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }
    
    /**
     * Handle logout exceptions (400 Bad Request)
     */
    @ExceptionHandler(LogoutException::class)
    fun handleLogoutException(ex: LogoutException): ResponseEntity<ErrorResponse> {
        logger.error("Logout error: {}", ex.message)
        
        val errorResponse = ErrorResponse(
            error = "LOGOUT_FAILED",
            message = ex.message ?: "Logout failed",
            status = HttpStatus.BAD_REQUEST.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }
    
    /**
     * Handle Google token validation exceptions (401 Unauthorized)
     */
    @ExceptionHandler(GoogleTokenValidationException::class)
    fun handleGoogleTokenValidationException(ex: GoogleTokenValidationException): ResponseEntity<ErrorResponse> {
        logger.error("Google token validation error: {}", ex.message)
        
        val errorResponse = ErrorResponse(
            error = "GOOGLE_TOKEN_INVALID",
            message = ex.message ?: "Google token validation failed",
            status = HttpStatus.UNAUTHORIZED.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }
    
    /**
     * Handle Apple token validation exceptions (401 Unauthorized)
     */
    @ExceptionHandler(AppleTokenValidationException::class)
    fun handleAppleTokenValidationException(ex: AppleTokenValidationException): ResponseEntity<ErrorResponse> {
        logger.error("Apple token validation error: {}", ex.message)
        
        val errorResponse = ErrorResponse(
            error = "APPLE_TOKEN_INVALID",
            message = ex.message ?: "Apple token validation failed",
            status = HttpStatus.UNAUTHORIZED.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }
    
    /**
     * Handle validation errors (400 Bad Request)
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        logger.error("Validation error: {}", ex.message)
        
        val fieldErrors = ex.bindingResult.fieldErrors
        val errorMessage = if (fieldErrors.isNotEmpty()) {
            fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        } else {
            "Validation failed"
        }
        
        val errorResponse = ErrorResponse(
            error = "VALIDATION_FAILED",
            message = errorMessage,
            status = HttpStatus.BAD_REQUEST.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }
    
    /**
     * Handle method argument type mismatch (400 Bad Request)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(ex: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
        logger.error("Method argument type mismatch: {}", ex.message)
        
        val errorResponse = ErrorResponse(
            error = "INVALID_REQUEST_FORMAT",
            message = "Invalid request format: ${ex.name} should be of type ${ex.requiredType?.simpleName}",
            status = HttpStatus.BAD_REQUEST.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }
    
    /**
     * Handle missing request header (400 Bad Request)
     */
    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException::class)
    fun handleMissingRequestHeaderException(ex: org.springframework.web.bind.MissingRequestHeaderException): ResponseEntity<ErrorResponse> {
        logger.error("Missing request header: {}", ex.message)
        
        val errorResponse = ErrorResponse(
            error = "MISSING_HEADER",
            message = "Missing required header: ${ex.headerName}",
            status = HttpStatus.BAD_REQUEST.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }
    
    /**
     * Handle JWT token exceptions (401 Unauthorized)
     */
    @ExceptionHandler(ExpiredJwtException::class)
    fun handleExpiredJwtException(ex: ExpiredJwtException): ResponseEntity<ErrorResponse> {
        SecurityLogger.logSecurityError("JWT_TOKEN_EXPIRED", "JWT token has expired")
        
        val errorResponse = ErrorResponse(
            error = "TOKEN_EXPIRED",
            message = "JWT token has expired",
            status = HttpStatus.UNAUTHORIZED.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }
    
    @ExceptionHandler(MalformedJwtException::class)
    fun handleMalformedJwtException(ex: MalformedJwtException): ResponseEntity<ErrorResponse> {
        logger.warn("Malformed JWT token: {}", ex.message)
        
        val errorResponse = ErrorResponse(
            error = "TOKEN_MALFORMED",
            message = "JWT token is malformed",
            status = HttpStatus.UNAUTHORIZED.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }
    
    @ExceptionHandler(SignatureException::class)
    fun handleSignatureException(ex: SignatureException): ResponseEntity<ErrorResponse> {
        logger.warn("JWT signature validation failed: {}", ex.message)
        
        val errorResponse = ErrorResponse(
            error = "TOKEN_SIGNATURE_INVALID",
            message = "JWT token signature is invalid",
            status = HttpStatus.UNAUTHORIZED.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }
    
    @ExceptionHandler(UnsupportedJwtException::class)
    fun handleUnsupportedJwtException(ex: UnsupportedJwtException): ResponseEntity<ErrorResponse> {
        logger.warn("Unsupported JWT token: {}", ex.message)
        
        val errorResponse = ErrorResponse(
            error = "TOKEN_UNSUPPORTED",
            message = "JWT token format is not supported",
            status = HttpStatus.UNAUTHORIZED.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }
    
    @ExceptionHandler(JwtException::class)
    fun handleJwtException(ex: JwtException): ResponseEntity<ErrorResponse> {
        logger.warn("JWT processing error: {}", ex.message)
        
        val errorResponse = ErrorResponse(
            error = "TOKEN_INVALID",
            message = "JWT token is invalid",
            status = HttpStatus.UNAUTHORIZED.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }
    
    /**
     * Handle Spring Security authentication exceptions (401 Unauthorized)
     */
    @ExceptionHandler(SpringAuthenticationException::class)
    fun handleSpringAuthenticationException(ex: SpringAuthenticationException): ResponseEntity<ErrorResponse> {
        logger.warn("Spring Security authentication failed: {}", ex.message)
        
        val errorResponse = ErrorResponse(
            error = "AUTHENTICATION_FAILED",
            message = "Authentication failed",
            status = HttpStatus.UNAUTHORIZED.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }
    
    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentialsException(ex: BadCredentialsException): ResponseEntity<ErrorResponse> {
        logger.warn("Bad credentials provided: {}", ex.message)
        
        val errorResponse = ErrorResponse(
            error = "INVALID_CREDENTIALS",
            message = "Invalid credentials provided",
            status = HttpStatus.UNAUTHORIZED.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }
    
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(ex: AccessDeniedException): ResponseEntity<ErrorResponse> {
        logger.warn("Access denied: {}", ex.message)
        
        val errorResponse = ErrorResponse(
            error = "ACCESS_DENIED",
            message = "Access denied",
            status = HttpStatus.FORBIDDEN.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse)
    }
    
    /**
     * Handle service unavailable errors (503 Service Unavailable)
     */
    @ExceptionHandler(ResourceAccessException::class)
    fun handleResourceAccessException(ex: ResourceAccessException): ResponseEntity<ErrorResponse> {
        logger.error("External service unavailable: {}", ex.message)
        
        val errorResponse = ErrorResponse(
            error = "SERVICE_UNAVAILABLE",
            message = "External authentication service is temporarily unavailable. Please try again later.",
            status = HttpStatus.SERVICE_UNAVAILABLE.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse)
    }
    
    @ExceptionHandler(RestClientException::class)
    fun handleRestClientException(ex: RestClientException): ResponseEntity<ErrorResponse> {
        logger.error("REST client error: {}", ex.message)
        
        val errorResponse = ErrorResponse(
            error = "SERVICE_UNAVAILABLE",
            message = "Authentication service is temporarily unavailable. Please try again later.",
            status = HttpStatus.SERVICE_UNAVAILABLE.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse)
    }
    
    @ExceptionHandler(ConnectException::class)
    fun handleConnectException(ex: ConnectException): ResponseEntity<ErrorResponse> {
        logger.error("Connection error: {}", ex.message)
        
        val errorResponse = ErrorResponse(
            error = "SERVICE_UNAVAILABLE",
            message = "Unable to connect to authentication service. Please try again later.",
            status = HttpStatus.SERVICE_UNAVAILABLE.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse)
    }
    
    @ExceptionHandler(SocketTimeoutException::class, TimeoutException::class)
    fun handleTimeoutException(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Request timeout: {}", ex.message)
        
        val errorResponse = ErrorResponse(
            error = "REQUEST_TIMEOUT",
            message = "Request timed out. Please try again later.",
            status = HttpStatus.REQUEST_TIMEOUT.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResponse)
    }
    
    /**
     * Handle database access errors (503 Service Unavailable)
     */
    @ExceptionHandler(DataAccessException::class)
    fun handleDataAccessException(ex: DataAccessException): ResponseEntity<ErrorResponse> {
        logger.error("Database access error: {}", ex.message)
        
        val errorResponse = ErrorResponse(
            error = "SERVICE_UNAVAILABLE",
            message = "Database service is temporarily unavailable. Please try again later.",
            status = HttpStatus.SERVICE_UNAVAILABLE.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse)
    }
    
    /**
     * Handle rate limiting exceptions (429 Too Many Requests)
     */
    @ExceptionHandler(RateLimitExceededException::class)
    fun handleRateLimitExceededException(ex: RateLimitExceededException): ResponseEntity<ErrorResponse> {
        // Note: In a real implementation, you would extract the endpoint and client IP from the request
        SecurityLogger.logRateLimitExceeded("unknown", "unknown")
        
        val errorResponse = ErrorResponse(
            error = "RATE_LIMIT_EXCEEDED",
            message = ex.message ?: "Rate limit exceeded. Too many requests.",
            status = HttpStatus.TOO_MANY_REQUESTS.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse)
    }
    
    /**
     * Handle malformed JSON requests (400 Bad Request)
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        logger.warn("Malformed JSON request: {}", ex.message)
        
        val errorResponse = ErrorResponse(
            error = "MALFORMED_JSON",
            message = "Request body contains malformed JSON",
            status = HttpStatus.BAD_REQUEST.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }
    
    /**
     * Handle resource not found (404 Not Found)
     */
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(ex: NoResourceFoundException): ResponseEntity<ErrorResponse> {
        logger.warn("Resource not found: {}", ex.message)
        
        val errorResponse = ErrorResponse(
            error = "RESOURCE_NOT_FOUND",
            message = "The requested resource was not found",
            status = HttpStatus.NOT_FOUND.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }
    
    /**
     * Handle file upload exceptions with appropriate HTTP status codes
     */
    @ExceptionHandler(FileUploadException::class)
    fun handleFileUploadException(ex: FileUploadException): ResponseEntity<ErrorResponse> {
        logger.error("File upload error [{}]: {}", ex.errorCode, ex.message)
        
        val httpStatus = when (ex.errorCode) {
            FileUploadException.FILE_TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE
            FileUploadException.INVALID_FILE_TYPE -> HttpStatus.UNSUPPORTED_MEDIA_TYPE
            FileUploadException.FILE_EMPTY, 
            FileUploadException.INVALID_FILE_NAME -> HttpStatus.BAD_REQUEST
            FileUploadException.SECURITY_VIOLATION -> {
                SecurityLogger.logSecurityError("FILE_SECURITY_VIOLATION", ex.message ?: "File security violation")
                HttpStatus.BAD_REQUEST
            }
            FileUploadException.STORAGE_ERROR,
            FileUploadException.CLEANUP_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR
            "FILE_NOT_FOUND" -> HttpStatus.NOT_FOUND
            "FILE_ACCESS_DENIED" -> HttpStatus.FORBIDDEN
            else -> HttpStatus.BAD_REQUEST
        }
        
        val errorResponse = ErrorResponse(
            error = ex.errorCode,
            message = ex.message ?: "File upload operation failed",
            status = httpStatus.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(httpStatus).body(errorResponse)
    }
    
    /**
     * Handle generic exceptions (500 Internal Server Error)
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error occurred", ex)
        
        val errorResponse = ErrorResponse(
            error = "INTERNAL_SERVER_ERROR",
            message = "An unexpected error occurred. Please try again later.",
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            timestamp = LocalDateTime.now()
        )
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}