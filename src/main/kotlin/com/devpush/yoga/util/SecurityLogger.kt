package com.devpush.yoga.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.*

/**
 * Utility class for structured logging with security considerations
 * Ensures sensitive information is masked in logs
 */
object SecurityLogger {
    
    private val logger: Logger = LoggerFactory.getLogger(SecurityLogger::class.java)
    
    /**
     * Log authentication attempt
     */
    fun logAuthenticationAttempt(provider: String, userId: String?, email: String?) {
        val traceId = generateTraceId()
        MDC.put("traceId", traceId)
        MDC.put("event", "AUTH_ATTEMPT")
        MDC.put("provider", provider)
        
        try {
            logger.info(
                "Authentication attempt - Provider: {}, UserId: {}, Email: {}",
                provider,
                maskUserId(userId),
                maskEmail(email)
            )
        } finally {
            MDC.clear()
        }
    }
    
    /**
     * Log successful authentication
     */
    fun logAuthenticationSuccess(provider: String, userId: String?, email: String?) {
        val traceId = generateTraceId()
        MDC.put("traceId", traceId)
        MDC.put("event", "AUTH_SUCCESS")
        MDC.put("provider", provider)
        
        try {
            logger.info(
                "Authentication successful - Provider: {}, UserId: {}, Email: {}",
                provider,
                maskUserId(userId),
                maskEmail(email)
            )
        } finally {
            MDC.clear()
        }
    }
    
    /**
     * Log authentication failure
     */
    fun logAuthenticationFailure(provider: String, reason: String, userId: String? = null, email: String? = null) {
        val traceId = generateTraceId()
        MDC.put("traceId", traceId)
        MDC.put("event", "AUTH_FAILURE")
        MDC.put("provider", provider)
        MDC.put("reason", reason)
        
        try {
            logger.warn(
                "Authentication failed - Provider: {}, Reason: {}, UserId: {}, Email: {}",
                provider,
                reason,
                maskUserId(userId),
                maskEmail(email)
            )
        } finally {
            MDC.clear()
        }
    }
    
    /**
     * Log token refresh attempt
     */
    fun logTokenRefreshAttempt(userId: String?) {
        val traceId = generateTraceId()
        MDC.put("traceId", traceId)
        MDC.put("event", "TOKEN_REFRESH_ATTEMPT")
        
        try {
            logger.info(
                "Token refresh attempt - UserId: {}",
                maskUserId(userId)
            )
        } finally {
            MDC.clear()
        }
    }
    
    /**
     * Log successful token refresh
     */
    fun logTokenRefreshSuccess(userId: String?) {
        val traceId = generateTraceId()
        MDC.put("traceId", traceId)
        MDC.put("event", "TOKEN_REFRESH_SUCCESS")
        
        try {
            logger.info(
                "Token refresh successful - UserId: {}",
                maskUserId(userId)
            )
        } finally {
            MDC.clear()
        }
    }
    
    /**
     * Log token refresh failure
     */
    fun logTokenRefreshFailure(reason: String, userId: String? = null) {
        val traceId = generateTraceId()
        MDC.put("traceId", traceId)
        MDC.put("event", "TOKEN_REFRESH_FAILURE")
        MDC.put("reason", reason)
        
        try {
            logger.warn(
                "Token refresh failed - Reason: {}, UserId: {}",
                reason,
                maskUserId(userId)
            )
        } finally {
            MDC.clear()
        }
    }
    
    /**
     * Log logout attempt
     */
    fun logLogoutAttempt(userId: String?) {
        val traceId = generateTraceId()
        MDC.put("traceId", traceId)
        MDC.put("event", "LOGOUT_ATTEMPT")
        
        try {
            logger.info(
                "Logout attempt - UserId: {}",
                maskUserId(userId)
            )
        } finally {
            MDC.clear()
        }
    }
    
    /**
     * Log successful logout
     */
    fun logLogoutSuccess(userId: String?) {
        val traceId = generateTraceId()
        MDC.put("traceId", traceId)
        MDC.put("event", "LOGOUT_SUCCESS")
        
        try {
            logger.info(
                "Logout successful - UserId: {}",
                maskUserId(userId)
            )
        } finally {
            MDC.clear()
        }
    }
    
    /**
     * Log security error
     */
    fun logSecurityError(event: String, error: String, details: Map<String, Any?> = emptyMap()) {
        val traceId = generateTraceId()
        MDC.put("traceId", traceId)
        MDC.put("event", "SECURITY_ERROR")
        MDC.put("errorType", event)
        
        try {
            val maskedDetails = details.mapValues { (key, value) ->
                when (key.lowercase()) {
                    "email" -> maskEmail(value?.toString())
                    "userid", "user_id" -> maskUserId(value?.toString())
                    "token", "jwt", "accesstoken", "refreshtoken" -> maskToken(value?.toString())
                    else -> value
                }
            }
            
            logger.error(
                "Security error - Event: {}, Error: {}, Details: {}",
                event,
                error,
                maskedDetails
            )
        } finally {
            MDC.clear()
        }
    }
    
    /**
     * Log rate limiting event
     */
    fun logRateLimitExceeded(endpoint: String, clientIp: String?) {
        val traceId = generateTraceId()
        MDC.put("traceId", traceId)
        MDC.put("event", "RATE_LIMIT_EXCEEDED")
        MDC.put("endpoint", endpoint)
        
        try {
            logger.warn(
                "Rate limit exceeded - Endpoint: {}, ClientIP: {}",
                endpoint,
                maskIpAddress(clientIp)
            )
        } finally {
            MDC.clear()
        }
    }
    
    /**
     * Mask email address for logging
     * Example: john.doe@example.com -> j***@e***.com
     */
    private fun maskEmail(email: String?): String? {
        if (email.isNullOrBlank()) return null
        
        val parts = email.split("@")
        if (parts.size != 2) return "***@***.***"
        
        val localPart = parts[0]
        val domainPart = parts[1]
        
        val maskedLocal = if (localPart.length <= 1) {
            "*"
        } else {
            "${localPart.first()}${"*".repeat(localPart.length - 1)}"
        }
        
        val maskedDomain = if (domainPart.length <= 1) {
            "*"
        } else {
            "${domainPart.first()}${"*".repeat(domainPart.length - 1)}"
        }
        
        return "$maskedLocal@$maskedDomain"
    }
    
    /**
     * Mask user ID for logging
     * Example: 123456789 -> 12***89
     */
    private fun maskUserId(userId: String?): String? {
        if (userId.isNullOrBlank()) return null
        
        return when {
            userId.length <= 2 -> "***"
            userId.length <= 4 -> "${userId.first()}***${userId.last()}"
            else -> "${userId.take(2)}***${userId.takeLast(2)}"
        }
    }
    
    /**
     * Mask token for logging
     * Example: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... -> eyJ***J9
     */
    private fun maskToken(token: String?): String? {
        if (token.isNullOrBlank()) return null
        
        return when {
            token.length <= 6 -> "***"
            else -> "${token.take(3)}***${token.takeLast(2)}"
        }
    }
    
    /**
     * Mask IP address for logging
     * Example: 192.168.1.100 -> 192.168.***.***
     */
    private fun maskIpAddress(ip: String?): String? {
        if (ip.isNullOrBlank()) return null
        
        val parts = ip.split(".")
        return if (parts.size == 4) {
            "${parts[0]}.${parts[1]}.***.***.***"
        } else {
            "***.***.***"
        }
    }
    
    /**
     * Generate a unique trace ID for request tracking
     */
    private fun generateTraceId(): String {
        return UUID.randomUUID().toString().replace("-", "").take(16)
    }
}