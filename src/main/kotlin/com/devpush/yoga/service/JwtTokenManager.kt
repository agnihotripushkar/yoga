package com.devpush.yoga.service

import com.devpush.yoga.entity.User
import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.Key
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtTokenManager(
    @Value("\${jwt.secret}")
    private val jwtSecret: String,
    
    @Value("\${jwt.access-token-expiration}")
    private val accessTokenExpiration: Long,
    
    @Value("\${jwt.refresh-token-expiration}")
    private val refreshTokenExpiration: Long
) {
    
    private val signingKey: SecretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    
    /**
     * Generate an access token for the given user
     */
    fun generateAccessToken(user: User): String {
        val now = Instant.now()
        val expiryDate = now.plus(accessTokenExpiration, ChronoUnit.MILLIS)
        
        return Jwts.builder()
            .subject(user.id.toString())
            .claim("email", user.email)
            .claim("name", user.name)
            .claim("provider", user.provider.name)
            .claim("type", "access")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiryDate))
            .signWith(signingKey)
            .compact()
    }
    
    /**
     * Generate a refresh token for the given user
     */
    fun generateRefreshToken(user: User): String {
        val now = Instant.now()
        val expiryDate = now.plus(refreshTokenExpiration, ChronoUnit.MILLIS)
        
        return Jwts.builder()
            .subject(user.id.toString())
            .claim("type", "refresh")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiryDate))
            .signWith(signingKey)
            .compact()
    }
    
    /**
     * Validate a JWT token and return the claims if valid
     */
    fun validateToken(token: String): Claims? {
        return try {
            Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (ex: JwtException) {
            null
        } catch (ex: IllegalArgumentException) {
            null
        }
    }
    
    /**
     * Extract user ID from a valid JWT token
     */
    fun getUserIdFromToken(token: String): Long? {
        val claims = validateToken(token)
        return claims?.subject?.toLongOrNull()
    }
    
    /**
     * Extract user email from a valid JWT token
     */
    fun getUserEmailFromToken(token: String): String? {
        val claims = validateToken(token)
        return claims?.get("email", String::class.java)
    }
    
    /**
     * Check if a token is expired
     */
    fun isTokenExpired(token: String): Boolean {
        val claims = validateToken(token) ?: return true
        return claims.expiration.before(Date())
    }
    
    /**
     * Check if a token is an access token
     */
    fun isAccessToken(token: String): Boolean {
        val claims = validateToken(token) ?: return false
        return claims.get("type", String::class.java) == "access"
    }
    
    /**
     * Check if a token is a refresh token
     */
    fun isRefreshToken(token: String): Boolean {
        val claims = validateToken(token) ?: return false
        return claims.get("type", String::class.java) == "refresh"
    }
    
    /**
     * Get the expiration time of a token in milliseconds
     */
    fun getTokenExpiration(token: String): Long? {
        val claims = validateToken(token) ?: return null
        return claims.expiration.time
    }
    
    /**
     * Extract all user information from an access token
     */
    fun getUserInfoFromAccessToken(token: String): UserTokenInfo? {
        val claims = validateToken(token) ?: return null
        
        if (!isAccessToken(token)) {
            return null
        }
        
        val userId = claims.subject?.toLongOrNull() ?: return null
        val email = claims.get("email", String::class.java) ?: return null
        val name = claims.get("name", String::class.java)
        val provider = claims.get("provider", String::class.java)
        
        return UserTokenInfo(
            id = userId,
            email = email,
            name = name,
            provider = provider
        )
    }
    
    /**
     * Data class to hold user information extracted from JWT token
     */
    data class UserTokenInfo(
        val id: Long,
        val email: String,
        val name: String?,
        val provider: String?
    )
}