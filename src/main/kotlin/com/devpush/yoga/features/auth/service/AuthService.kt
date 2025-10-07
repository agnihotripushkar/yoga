package com.devpush.yoga.features.auth.service

import com.devpush.yoga.features.auth.dto.AuthResponse
import com.devpush.yoga.features.auth.dto.UserProfile
import com.devpush.yoga.entity.User
import com.devpush.yoga.service.UserService
import com.devpush.yoga.util.SecurityLogger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AuthService(
    private val googleTokenValidator: GoogleTokenValidator,
    private val appleTokenValidator: AppleTokenValidator,
    private val userService: UserService,
    private val jwtTokenManager: JwtTokenManager,
    private val refreshTokenService: RefreshTokenService
) {
    
    private val logger = LoggerFactory.getLogger(AuthService::class.java)
    
    /**
     * Authenticate user with Google OAuth token
     */
    fun authenticateWithGoogle(idToken: String): AuthResponse {
        logger.info("Starting Google OAuth authentication")
        
        try {
            val oAuthUserInfo = googleTokenValidator.validateToken(idToken)
            SecurityLogger.logAuthenticationAttempt("GOOGLE", oAuthUserInfo.providerId, oAuthUserInfo.email)
            
            val user = userService.createOrUpdateUser(oAuthUserInfo)
            logger.info("User created/updated successfully with id: {}", user.id)
            
            val accessToken = jwtTokenManager.generateAccessToken(user)
            val refreshTokenEntity = refreshTokenService.createRefreshToken(user)
            
            val expiresIn = jwtTokenManager.getTokenExpiration(accessToken)?.let { expiration ->
                (expiration - System.currentTimeMillis()) / 1000
            } ?: 900L
            
            val userProfile = userService.toUserProfile(user)
            
            SecurityLogger.logAuthenticationSuccess("GOOGLE", user.id.toString(), user.email)
            
            return AuthResponse(
                accessToken = accessToken,
                refreshToken = refreshTokenEntity.token,
                expiresIn = expiresIn,
                user = userProfile
            )
            
        } catch (ex: GoogleTokenValidationException) {
            SecurityLogger.logAuthenticationFailure("GOOGLE", "Token validation failed: ${ex.message}")
            throw AuthenticationException("Google authentication failed: ${ex.message}", ex)
        } catch (ex: Exception) {
            SecurityLogger.logAuthenticationFailure("GOOGLE", "Unexpected error: ${ex.message}")
            throw AuthenticationException("Authentication failed: ${ex.message}", ex)
        }
    }
    
    /**
     * Authenticate user with Apple OAuth token
     */
    fun authenticateWithApple(idToken: String): AuthResponse {
        logger.info("Starting Apple OAuth authentication")
        
        try {
            val oAuthUserInfo = appleTokenValidator.validateToken(idToken)
            SecurityLogger.logAuthenticationAttempt("APPLE", oAuthUserInfo.providerId, oAuthUserInfo.email)
            
            val user = userService.createOrUpdateUser(oAuthUserInfo)
            logger.info("User created/updated successfully with id: {}", user.id)
            
            val accessToken = jwtTokenManager.generateAccessToken(user)
            val refreshTokenEntity = refreshTokenService.createRefreshToken(user)
            
            val expiresIn = jwtTokenManager.getTokenExpiration(accessToken)?.let { expiration ->
                (expiration - System.currentTimeMillis()) / 1000
            } ?: 900L
            
            val userProfile = userService.toUserProfile(user)
            
            SecurityLogger.logAuthenticationSuccess("APPLE", user.id.toString(), user.email)
            
            return AuthResponse(
                accessToken = accessToken,
                refreshToken = refreshTokenEntity.token,
                expiresIn = expiresIn,
                user = userProfile
            )
            
        } catch (ex: AppleTokenValidationException) {
            SecurityLogger.logAuthenticationFailure("APPLE", "Token validation failed: ${ex.message}")
            throw AuthenticationException("Apple authentication failed: ${ex.message}", ex)
        } catch (ex: Exception) {
            SecurityLogger.logAuthenticationFailure("APPLE", "Unexpected error: ${ex.message}")
            throw AuthenticationException("Authentication failed: ${ex.message}", ex)
        }
    }
    
    /**
     * Refresh JWT tokens using a valid refresh token
     */
    fun refreshTokens(refreshToken: String): AuthResponse {
        logger.info("Starting token refresh")
        
        try {
            val refreshTokenEntity = refreshTokenService.validateToken(refreshToken)
                .orElseThrow { 
                    SecurityLogger.logTokenRefreshFailure("Invalid or expired refresh token")
                    TokenRefreshException("Invalid or expired refresh token")
                }
            
            val user = refreshTokenEntity.user
            SecurityLogger.logTokenRefreshAttempt(user.id.toString())
            
            val newAccessToken = jwtTokenManager.generateAccessToken(user)
            val newRefreshTokenEntity = refreshTokenService.createRefreshToken(user)
            
            refreshTokenService.revokeToken(refreshToken)
            logger.debug("Old refresh token revoked for user: {}", user.id)
            
            val expiresIn = jwtTokenManager.getTokenExpiration(newAccessToken)?.let { expiration ->
                (expiration - System.currentTimeMillis()) / 1000
            } ?: 900L
            
            val userProfile = userService.toUserProfile(user)
            
            SecurityLogger.logTokenRefreshSuccess(user.id.toString())
            
            return AuthResponse(
                accessToken = newAccessToken,
                refreshToken = newRefreshTokenEntity.token,
                expiresIn = expiresIn,
                user = userProfile
            )
            
        } catch (ex: TokenRefreshException) {
            SecurityLogger.logTokenRefreshFailure("Token refresh exception: ${ex.message}")
            throw ex
        } catch (ex: Exception) {
            SecurityLogger.logTokenRefreshFailure("Unexpected error: ${ex.message}")
            throw AuthenticationException("Token refresh failed: ${ex.message}", ex)
        }
    }
    
    /**
     * Logout user by revoking their refresh token
     */
    fun logout(refreshToken: String) {
        logger.info("Starting user logout")
        
        try {
            val refreshTokenEntity = refreshTokenService.validateToken(refreshToken)
            val userId = refreshTokenEntity.map { it.user.id.toString() }.orElse(null)
            
            SecurityLogger.logLogoutAttempt(userId)
            
            val revoked = refreshTokenService.revokeToken(refreshToken)
            
            if (revoked) {
                SecurityLogger.logLogoutSuccess(userId)
            } else {
                logger.warn("Logout attempted with non-existent refresh token")
                SecurityLogger.logLogoutSuccess(null)
            }
            
        } catch (ex: Exception) {
            SecurityLogger.logSecurityError("LOGOUT_ERROR", ex.message ?: "Unknown error")
            throw LogoutException("Logout failed: ${ex.message}", ex)
        }
    }
}

/**
 * Exception thrown when authentication process fails
 */
class AuthenticationException(message: String, cause: Throwable? = null) : 
    RuntimeException(message, cause)

/**
 * Exception thrown when token refresh fails
 */
class TokenRefreshException(message: String, cause: Throwable? = null) : 
    RuntimeException(message, cause)

/**
 * Exception thrown when logout process fails
 */
class LogoutException(message: String, cause: Throwable? = null) : 
    RuntimeException(message, cause)