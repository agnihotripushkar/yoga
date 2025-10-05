package com.devpush.yoga.repository

import com.devpush.yoga.entity.RefreshToken
import com.devpush.yoga.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    
    /**
     * Find refresh token by token string
     * Used for token validation during refresh operations
     */
    fun findByToken(token: String): Optional<RefreshToken>
    
    /**
     * Find all refresh tokens for a specific user
     * Used for user session management
     */
    fun findByUser(user: User): List<RefreshToken>
    
    /**
     * Find all valid (non-revoked and non-expired) refresh tokens for a user
     * Used for session management and security
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user AND rt.revoked = false AND rt.expiresAt > :now")
    fun findValidTokensByUser(@Param("user") user: User, @Param("now") now: LocalDateTime): List<RefreshToken>
    
    /**
     * Delete all refresh tokens for a specific user
     * Used during logout all sessions or account deletion
     */
    fun deleteByUser(user: User): Int
    
    /**
     * Delete refresh token by token string
     * Used during logout of specific session
     */
    fun deleteByToken(token: String): Int
    
    /**
     * Revoke (mark as revoked) all refresh tokens for a user
     * Used for security purposes when user changes password or suspicious activity
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user")
    fun revokeAllTokensByUser(@Param("user") user: User): Int
    
    /**
     * Revoke (mark as revoked) a specific refresh token
     * Used during logout of specific session
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.token = :token")
    fun revokeTokenByToken(@Param("token") token: String): Int
    
    /**
     * Delete all expired refresh tokens
     * Used for cleanup operations to maintain database performance
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    fun deleteExpiredTokens(@Param("now") now: LocalDateTime): Int
    
    /**
     * Delete all revoked refresh tokens older than specified date
     * Used for cleanup operations to maintain database performance
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.revoked = true AND rt.createdAt < :cutoffDate")
    fun deleteRevokedTokensOlderThan(@Param("cutoffDate") cutoffDate: LocalDateTime): Int
    
    /**
     * Check if a refresh token exists and is valid
     * Used for quick validation checks
     */
    @Query("SELECT COUNT(rt) > 0 FROM RefreshToken rt WHERE rt.token = :token AND rt.revoked = false AND rt.expiresAt > :now")
    fun existsValidToken(@Param("token") token: String, @Param("now") now: LocalDateTime): Boolean
}