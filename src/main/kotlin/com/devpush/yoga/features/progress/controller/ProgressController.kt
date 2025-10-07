package com.devpush.yoga.features.progress.controller

import com.devpush.yoga.features.progress.dto.*
import com.devpush.yoga.features.progress.service.ProgressService
import com.devpush.yoga.features.auth.service.JwtTokenManager
import com.devpush.yoga.service.RateLimitService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/progress")
class ProgressController(
    private val progressService: ProgressService,
    private val jwtTokenManager: JwtTokenManager,
    private val rateLimitService: RateLimitService
) {
    
    private val logger = LoggerFactory.getLogger(ProgressController::class.java)
    
    /**
     * Record a completed yoga session
     */
    @PostMapping("/session")
    fun recordSession(
        @RequestHeader("Authorization") authorization: String,
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
    
    /**
     * Get overall progress summary
     */
    @GetMapping("/summary")
    fun getProgressSummary(
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
    
    /**
     * Get weekly progress data
     */
    @GetMapping("/weekly")
    fun getWeeklyProgress(
        @RequestHeader("Authorization") authorization: String,
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
    
    /**
     * Get monthly progress data
     */
    @GetMapping("/monthly")
    fun getMonthlyProgress(
        @RequestHeader("Authorization") authorization: String,
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
    private fun extractAndValidateUserId(authorization: String): Long {
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