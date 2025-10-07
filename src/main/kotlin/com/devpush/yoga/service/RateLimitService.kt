package com.devpush.yoga.service

import com.devpush.yoga.exception.RateLimitExceededException
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for managing rate limiting using token bucket algorithm
 */
@Service
class RateLimitService {
    
    private val buckets = ConcurrentHashMap<String, Bucket>()
    
    /**
     * Check if request is allowed for authentication endpoints
     * @param clientId Identifier for the client (IP address or user ID)
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    fun checkAuthenticationRateLimit(clientId: String) {
        val bucket = getOrCreateAuthBucket(clientId)
        
        if (!bucket.tryConsume(1)) {
            throw RateLimitExceededException("Authentication rate limit exceeded. Please try again later.")
        }
    }
    
    /**
     * Check if request is allowed for profile update endpoints
     * @param clientId Identifier for the client (user ID)
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    fun checkProfileUpdateRateLimit(clientId: String) {
        val bucket = getOrCreateProfileBucket(clientId)
        
        if (!bucket.tryConsume(1)) {
            throw RateLimitExceededException("Profile update rate limit exceeded. Please try again later.")
        }
    }
    
    /**
     * Check if request is allowed for file upload endpoints
     * @param clientId Identifier for the client (user ID)
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    fun checkFileUploadRateLimit(clientId: String) {
        val bucket = getOrCreateFileUploadBucket(clientId)
        
        if (!bucket.tryConsume(1)) {
            throw RateLimitExceededException("File upload rate limit exceeded. Please try again later.")
        }
    }
    
    /**
     * Check if request is allowed for progress query endpoints
     * @param clientId Identifier for the client (user ID)
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    fun checkProgressQueryRateLimit(clientId: String) {
        val bucket = getOrCreateProgressBucket(clientId)
        
        if (!bucket.tryConsume(1)) {
            throw RateLimitExceededException("Progress query rate limit exceeded. Please try again later.")
        }
    }
    
    /**
     * Check if request is allowed for class search endpoints
     * @param clientId Identifier for the client (user ID)
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    fun checkClassSearchRateLimit(clientId: String) {
        val bucket = getOrCreateClassSearchBucket(clientId)
        
        if (!bucket.tryConsume(1)) {
            throw RateLimitExceededException("Class search rate limit exceeded. Please try again later.")
        }
    }
    
    /**
     * Get or create a rate limiting bucket for authentication endpoints
     * Allows 10 requests per minute and 100 requests per hour
     */
    private fun getOrCreateAuthBucket(clientId: String): Bucket {
        return buckets.computeIfAbsent("auth:$clientId") {
            val perMinuteLimit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)))
            val perHourLimit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofHours(1)))
            
            Bucket.builder()
                .addLimit(perMinuteLimit)
                .addLimit(perHourLimit)
                .build()
        }
    }
    
    /**
     * Get or create a rate limiting bucket for profile update endpoints
     * Allows 10 requests per minute (profile updates are resource intensive)
     */
    private fun getOrCreateProfileBucket(clientId: String): Bucket {
        return buckets.computeIfAbsent("profile:$clientId") {
            val perMinuteLimit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)))
            val perHourLimit = Bandwidth.classic(50, Refill.intervally(50, Duration.ofHours(1)))
            
            Bucket.builder()
                .addLimit(perMinuteLimit)
                .addLimit(perHourLimit)
                .build()
        }
    }
    
    /**
     * Get or create a rate limiting bucket for file upload endpoints
     * Allows 5 requests per minute (file uploads are very resource intensive)
     */
    private fun getOrCreateFileUploadBucket(clientId: String): Bucket {
        return buckets.computeIfAbsent("upload:$clientId") {
            val perMinuteLimit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)))
            val perHourLimit = Bandwidth.classic(20, Refill.intervally(20, Duration.ofHours(1)))
            
            Bucket.builder()
                .addLimit(perMinuteLimit)
                .addLimit(perHourLimit)
                .build()
        }
    }
    
    /**
     * Get or create a rate limiting bucket for progress query endpoints
     * Allows 60 requests per minute (analytics queries can be frequent)
     */
    private fun getOrCreateProgressBucket(clientId: String): Bucket {
        return buckets.computeIfAbsent("progress:$clientId") {
            val perMinuteLimit = Bandwidth.classic(60, Refill.intervally(60, Duration.ofMinutes(1)))
            val perHourLimit = Bandwidth.classic(500, Refill.intervally(500, Duration.ofHours(1)))
            
            Bucket.builder()
                .addLimit(perMinuteLimit)
                .addLimit(perHourLimit)
                .build()
        }
    }
    
    /**
     * Get or create a rate limiting bucket for class search endpoints
     * Allows 100 requests per minute (search queries should be fast and frequent)
     */
    private fun getOrCreateClassSearchBucket(clientId: String): Bucket {
        return buckets.computeIfAbsent("search:$clientId") {
            val perMinuteLimit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)))
            val perHourLimit = Bandwidth.classic(1000, Refill.intervally(1000, Duration.ofHours(1)))
            
            Bucket.builder()
                .addLimit(perMinuteLimit)
                .addLimit(perHourLimit)
                .build()
        }
    }
    
    /**
     * Reset rate limit for a specific client and category (useful for testing or admin operations)
     */
    fun resetRateLimit(clientId: String, category: String = "auth") {
        buckets.remove("$category:$clientId")
    }
    
    /**
     * Reset all rate limits for a specific client
     */
    fun resetAllRateLimits(clientId: String) {
        val categories = listOf("auth", "profile", "upload", "progress", "search")
        categories.forEach { category ->
            buckets.remove("$category:$clientId")
        }
    }
    
    /**
     * Get remaining tokens for a client and category (useful for debugging)
     */
    fun getRemainingTokens(clientId: String, category: String = "auth"): Long {
        val bucket = buckets["$category:$clientId"]
        return bucket?.availableTokens ?: when (category) {
            "auth" -> 10L
            "profile" -> 10L
            "upload" -> 5L
            "progress" -> 60L
            "search" -> 100L
            else -> 0L
        }
    }
    
    /**
     * Get rate limit status for all categories for a client
     */
    fun getRateLimitStatus(clientId: String): Map<String, Long> {
        val categories = listOf("auth", "profile", "upload", "progress", "search")
        return categories.associateWith { category ->
            getRemainingTokens(clientId, category)
        }
    }
}