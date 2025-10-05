package com.devpush.yoga.controller

import com.devpush.yoga.dto.ErrorResponse
import com.devpush.yoga.service.AuthenticationException
import com.devpush.yoga.service.TokenRefreshException
import com.devpush.yoga.service.LogoutException
import com.devpush.yoga.service.GoogleTokenValidationException
import com.devpush.yoga.service.AppleTokenValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {
    
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    
    /**
     * Handle authentication exceptions (401 Unauthorized)
     */
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ResponseEntity<ErrorResponse> {
        logger.error("Authentication error: {}", ex.message)
        
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