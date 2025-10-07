package com.devpush.yoga.features.auth.service

import com.devpush.yoga.entity.RefreshToken
import com.devpush.yoga.entity.User
import com.devpush.yoga.repository.RefreshTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    @Value("\${jwt.refresh-token-expiration:604800000}") // 7 days in milliseconds
    private val refreshTokenExpirationMs: Long
) {
    
    private val logger = LoggerFactory.getLogger(RefreshTokenService::class.java)
    private val secureRandom = SecureRandom()
    
    /**
     * Create a new refresh token for the given user
     */
    fun createRefreshToken(user: User): RefreshToken {
        logger.debug("Creating refresh token for user: {}", user.id)
        
        val token = generateSecureToken()
        val expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000)
        
        val refreshToken = RefreshToken(
            token = token,
            user = user,
            expiresAt = expiresAt
        )
        
        val savedToken = refreshTokenRepository.save(refreshToken)
        logger.info("Created refresh token for user: {} with expiration: {}", user.id, expiresAt)
        return savedToken
    }
    
    /**
     * Find and validate refresh token by token string
     */
    fun findByToken(token: String): Optional<RefreshToken> {
        logger.debug("Finding refresh token by token")
        return refreshTokenRepository.findByToken(token)
    }
    
    /**
     * Validate if refresh token is valid (not revoked and not expired)
     */
    fun isValidToken(refreshToken: RefreshToken): Boolean {
        val isValid = refreshToken.isValid()
        logger.debug("Refresh token validation result: {} for user: {}", isValid, refreshToken.user.id)
        return isValid
    }
    
    /**
     * Validate refresh token by token string
     */
    fun validateToken(token: String): Optional<RefreshToken> {
        logger.debug("Validating refresh token")
        
        val refreshTokenOpt = findByToken(token)
        if (refreshTokenOpt.isEmpty) {
            logger.warn("Refresh token not found")
            return Optional.empty()
        }
        
        val refreshToken = refreshTokenOpt.get()
        if (!isValidToken(refreshToken)) {
            logger.warn("Invalid refresh token for user: {}", refreshToken.user.id)
            return Optional.empty()
        }
        
        return refreshTokenOpt
    }
    
    /**
     * Revoke a specific refresh token
     */
    fun revokeToken(token: String): Boolean {
        logger.debug("Revoking refresh token")
        
        val refreshTokenOpt = findByToken(token)
        if (refreshTokenOpt.isEmpty) {
            logger.warn("Attempted to revoke non-existent refresh token")
            return false
        }
        
        val refreshToken = refreshTokenOpt.get()
        refreshToken.revoked = true
        refreshTokenRepository.save(refreshToken)
        
        logger.info("Revoked refresh token for user: {}", refreshToken.user.id)
        return true
    }
    
    /**
     * Revoke all refresh tokens for a specific user
     */
    fun revokeAllTokensForUser(user: User): Int {
        logger.info("Revoking all refresh tokens for user: {}", user.id)
        val revokedCount = refreshTokenRepository.revokeAllTokensByUser(user)
        logger.info("Revoked {} refresh tokens for user: {}", revokedCount, user.id)
        return revokedCount
    }
    
    /**
     * Delete a specific refresh token
     */
    fun deleteToken(token: String): Boolean {
        logger.debug("Deleting refresh token")
        val deletedCount = refreshTokenRepository.deleteByToken(token)
        val deleted = deletedCount > 0
        
        if (deleted) {
            logger.info("Deleted refresh token")
        } else {
            logger.warn("Attempted to delete non-existent refresh token")
        }
        
        return deleted
    }
    
    /**
     * Delete all refresh tokens for a specific user
     */
    fun deleteAllTokensForUser(user: User): Int {
        logger.info("Deleting all refresh tokens for user: {}", user.id)
        val deletedCount = refreshTokenRepository.deleteByUser(user)
        logger.info("Deleted {} refresh tokens for user: {}", deletedCount, user.id)
        return deletedCount
    }
    
    /**
     * Get all valid refresh tokens for a user
     */
    fun getValidTokensForUser(user: User): List<RefreshToken> {
        logger.debug("Getting valid refresh tokens for user: {}", user.id)
        return refreshTokenRepository.findValidTokensByUser(user, LocalDateTime.now())
    }
    
    /**
     * Cleanup expired refresh tokens
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    fun cleanupExpiredTokens() {
        logger.debug("Starting cleanup of expired refresh tokens")
        val deletedCount = refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now())
        if (deletedCount > 0) {
            logger.info("Cleaned up {} expired refresh tokens", deletedCount)
        }
    }
    
    /**
     * Cleanup old revoked refresh tokens
     */
    @Scheduled(fixedRate = 86400000) // Run daily
    fun cleanupOldRevokedTokens() {
        logger.debug("Starting cleanup of old revoked refresh tokens")
        val cutoffDate = LocalDateTime.now().minusDays(30) // Keep revoked tokens for 30 days
        val deletedCount = refreshTokenRepository.deleteRevokedTokensOlderThan(cutoffDate)
        if (deletedCount > 0) {
            logger.info("Cleaned up {} old revoked refresh tokens", deletedCount)
        }
    }
    
    /**
     * Generate a cryptographically secure random token
     */
    private fun generateSecureToken(): String {
        val bytes = ByteArray(64) // 512 bits
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}