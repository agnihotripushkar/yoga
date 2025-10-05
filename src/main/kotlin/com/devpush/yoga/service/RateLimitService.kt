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
     * Reset rate limit for a specific client (useful for testing or admin operations)
     */
    fun resetRateLimit(clientId: String) {
        buckets.remove("auth:$clientId")
    }
    
    /**
     * Get remaining tokens for a client (useful for debugging)
     */
    fun getRemainingTokens(clientId: String): Long {
        val bucket = buckets["auth:$clientId"]
        return bucket?.availableTokens ?: 10L
    }
}