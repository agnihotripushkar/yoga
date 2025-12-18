package com.devpush.yoga.features.progress.controller

import com.devpush.yoga.features.progress.dto.*
import com.devpush.yoga.features.progress.service.ProgressService
import com.devpush.yoga.features.auth.service.JwtTokenManager
import com.devpush.yoga.service.RateLimitService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/progress")
@Tag(name = "Progress Tracking", description = "Yoga session tracking and progress analytics")
class ProgressController(
    private val progressService: ProgressService,
    private val jwtTokenManager: JwtTokenManager,
    private val rateLimitService: RateLimitService
) {
    
    private val logger = LoggerFactory.getLogger(ProgressController::class.java)
    
    @Operation(
        summary = "Record Yoga Session",
        description = "Record a completed yoga session with duration and class information",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Session recorded successfully",
                content = [Content(
                    schema = Schema(implementation = SessionResponse::class),
                    examples = [ExampleObject(
                        value = """
                        {
                          "id": "123e4567-e89b-12d3-a456-426614174000",
                          "durationMinutes": 45,
                          "caloriesBurned": 180,
                          "completedAt": "2024-01-15T10:30:00Z",
                          "yogaClass": {
                            "id": "123e4567-e89b-12d3-a456-426614174001",
                            "title": "Morning Flow"
                          }
                        }
                        """
                    )]
                )]
            ),
            ApiResponse(responseCode = "400", description = "Invalid session data"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    @PostMapping("/session")
    fun recordSession(
        @Parameter(description = "JWT access token")
        @RequestHeader("Authorization") authorization: String,
        @Parameter(description = "Session recording data")
        @Valid @RequestBody sessionRequest: SessionRecordRequest
    ): ResponseEntity<SessionResponse> {
        logger.info("Received session recording request")
        
        return try {
            val userId = extractAndValidateUserId(authorization)
            
            val sessionResponse = progressService.recordSession(userId, sessionRequest)
            logger.info("Successfully recorded session for userId: {}", userId)
            
            ResponseEntity.status(HttpStatus.CREATED).body(sessionResponse)
        } catch (ex: Exception) {
            logger.error("Session recording failed: {}", ex.message)
            throw RuntimeException("Session recording failed", ex)
        }
    }
    
    @Operation(
        summary = "Get Progress Summary",
        description = "Retrieve overall progress statistics including total sessions, duration, and calories",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Progress summary retrieved successfully",
                content = [Content(schema = Schema(implementation = ProgressSummary::class))]
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "429", description = "Rate limit exceeded")
        ]
    )
    @GetMapping("/summary")
    fun getProgressSummary(
        @Parameter(description = "JWT access token")
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<ProgressSummary> {
        logger.debug("Received progress summary request")
        
        return try {
            val userId = extractAndValidateUserId(authorization)
            
            // Apply rate limiting for progress queries
            rateLimitService.checkProgressQueryRateLimit(userId.toString())
            
            val progressSummary = progressService.getProgressSummary(userId)
            logger.debug("Successfully retrieved progress summary for userId: {}", userId)
            
            ResponseEntity.ok(progressSummary)
        } catch (ex: Exception) {
            logger.error("Progress summary retrieval failed: {}", ex.message)
            throw RuntimeException("Progress summary retrieval failed", ex)
        }
    }
    
    @Operation(
        summary = "Get Weekly Progress",
        description = "Retrieve weekly progress data with optional week offset (0 = current week, 1 = last week, etc.)",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Weekly progress retrieved successfully",
                content = [Content(schema = Schema(implementation = WeeklyProgress::class))]
            ),
            ApiResponse(responseCode = "400", description = "Invalid week offset"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "429", description = "Rate limit exceeded")
        ]
    )
    @GetMapping("/weekly")
    fun getWeeklyProgress(
        @Parameter(description = "JWT access token")
        @RequestHeader("Authorization") authorization: String,
        @Parameter(description = "Week offset (0 = current week, 1 = last week, max 52)", example = "0")
        @RequestParam(defaultValue = "0") weekOffset: Int
    ): ResponseEntity<WeeklyProgress> {
        logger.debug("Received weekly progress request with offset: {}", weekOffset)
        
        return try {
            val userId = extractAndValidateUserId(authorization)
            
            // Apply rate limiting for progress queries
            rateLimitService.checkProgressQueryRateLimit(userId.toString())
            
            // Validate week offset
            if (weekOffset < 0) {
                throw IllegalArgumentException("Week offset cannot be negative")
            }
            if (weekOffset > 52) {
                throw IllegalArgumentException("Week offset cannot exceed 52 weeks")
            }
            
            val weeklyProgress = progressService.getWeeklyProgress(userId, weekOffset)
            logger.debug("Successfully retrieved weekly progress for userId: {} with offset: {}", userId, weekOffset)
            
            ResponseEntity.ok(weeklyProgress)
        } catch (ex: Exception) {
            logger.error("Weekly progress retrieval failed: {}", ex.message)
            throw RuntimeException("Weekly progress retrieval failed", ex)
        }
    }
    
    @Operation(
        summary = "Get Monthly Progress",
        description = "Retrieve monthly progress data with optional month offset (0 = current month, 1 = last month, etc.)",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Monthly progress retrieved successfully",
                content = [Content(schema = Schema(implementation = MonthlyProgress::class))]
            ),
            ApiResponse(responseCode = "400", description = "Invalid month offset"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "429", description = "Rate limit exceeded")
        ]
    )
    @GetMapping("/monthly")
    fun getMonthlyProgress(
        @Parameter(description = "JWT access token")
        @RequestHeader("Authorization") authorization: String,
        @Parameter(description = "Month offset (0 = current month, 1 = last month, max 24)", example = "0")
        @RequestParam(defaultValue = "0") monthOffset: Int
    ): ResponseEntity<MonthlyProgress> {
        logger.debug("Received monthly progress request with offset: {}", monthOffset)
        
        return try {
            val userId = extractAndValidateUserId(authorization)
            
            // Apply rate limiting for progress queries
            rateLimitService.checkProgressQueryRateLimit(userId.toString())
            
            // Validate month offset
            if (monthOffset < 0) {
                throw IllegalArgumentException("Month offset cannot be negative")
            }
            if (monthOffset > 24) {
                throw IllegalArgumentException("Month offset cannot exceed 24 months")
            }
            
            val monthlyProgress = progressService.getMonthlyProgress(userId, monthOffset)
            logger.debug("Successfully retrieved monthly progress for userId: {} with offset: {}", userId, monthOffset)
            
            ResponseEntity.ok(monthlyProgress)
        } catch (ex: Exception) {
            logger.error("Monthly progress retrieval failed: {}", ex.message)
            throw RuntimeException("Monthly progress retrieval failed", ex)
        }
    }
    
    /**
     * Extract and validate user ID from JWT token
     */
    private fun extractAndValidateUserId(authorization: String): UUID {
        // Extract JWT token from Authorization header
        val token = extractTokenFromHeader(authorization)
            ?: throw RuntimeException("Invalid authorization header format")
        
        // Validate token and extract user ID
        val userId = jwtTokenManager.getUserIdFromToken(token)
            ?: throw RuntimeException("Invalid or expired access token")
        
        // Verify it's an access token
        if (!jwtTokenManager.isAccessToken(token)) {
            throw RuntimeException("Invalid token type. Access token required")
        }
        
        return userId
    }
    
    /**
     * Extract JWT token from Authorization header
     * Expected format: "Bearer <token>"
     */
    private fun extractTokenFromHeader(authorization: String): String? {
        return if (authorization.startsWith("Bearer ", ignoreCase = true)) {
            authorization.substring(7)
        } else {
            null
        }
    }
}