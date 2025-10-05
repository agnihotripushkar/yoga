package com.devpush.yoga.service

import com.devpush.yoga.dto.AuthResponse
import com.devpush.yoga.dto.UserProfile
import com.devpush.yoga.entity.User
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
     * Validates Google ID token, creates/updates user, and returns JWT tokens
     * 
     * @param idToken Google ID token from client
     * @return AuthResponse containing JWT tokens and user profile
     * @throws GoogleTokenValidationException if token validation fails
     * @throws AuthenticationException if authentication process fails
     */
    fun authenticateWithGoogle(idToken: String): AuthResponse {
        logger.info("Starting Google OAuth authentication")
        
        try {
            // Validate Google ID token and extract user information
            val oAuthUserInfo = googleTokenValidator.validateToken(idToken)
            SecurityLogger.logAuthenticationAttempt("GOOGLE", oAuthUserInfo.id, oAuthUserInfo.email)
            
            // Create or update user in database
            val user = userService.createOrUpdateUser(oAuthUserInfo)
            logger.info("User created/updated successfully with id: {}", user.id)
            
            // Generate JWT tokens
            val accessToken = jwtTokenManager.generateAccessToken(user)
            val refreshTokenEntity = refreshTokenService.createRefreshToken(user)
            
            // Calculate token expiration in seconds
            val expiresIn = jwtTokenManager.getTokenExpiration(accessToken)?.let { expiration ->
                (expiration - System.currentTimeMillis()) / 1000
            } ?: 900L // Default 15 minutes
            
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
     * Validates Apple ID token, creates/updates user, and returns JWT tokens
     * 
     * @param idToken Apple ID token from client
     * @return AuthResponse containing JWT tokens and user profile
     * @throws AppleTokenValidationException if token validation fails
     * @throws AuthenticationException if authentication process fails
     */
    fun authenticateWithApple(idToken: String): AuthResponse {
        logger.info("Starting Apple OAuth authentication")
        
        try {
            // Validate Apple ID token and extract user information
            val oAuthUserInfo = appleTokenValidator.validateToken(idToken)
            SecurityLogger.logAuthenticationAttempt("APPLE", oAuthUserInfo.id, oAuthUserInfo.email)
            
            // Create or update user in database
            val user = userService.createOrUpdateUser(oAuthUserInfo)
            logger.info("User created/updated successfully with id: {}", user.id)
            
            // Generate JWT tokens
            val accessToken = jwtTokenManager.generateAccessToken(user)
            val refreshTokenEntity = refreshTokenService.createRefreshToken(user)
            
            // Calculate token expiration in seconds
            val expiresIn = jwtTokenManager.getTokenExpiration(accessToken)?.let { expiration ->
                (expiration - System.currentTimeMillis()) / 1000
            } ?: 900L // Default 15 minutes
            
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
     * Validates refresh token, generates new tokens, and returns updated auth response
     * 
     * @param refreshToken The refresh token to validate and use for new token generation
     * @return AuthResponse containing new JWT tokens and user profile
     * @throws TokenRefreshException if refresh token is invalid or expired
     * @throws AuthenticationException if token refresh process fails
     */
    fun refreshTokens(refreshToken: String): AuthResponse {
        logger.info("Starting token refresh")
        
        try {
            // Validate refresh token
            val refreshTokenEntity = refreshTokenService.validateToken(refreshToken)
                .orElseThrow { 
                    SecurityLogger.logTokenRefreshFailure("Invalid or expired refresh token")
                    TokenRefreshException("Invalid or expired refresh token")
                }
            
            val user = refreshTokenEntity.user
            SecurityLogger.logTokenRefreshAttempt(user.id.toString())
            
            // Generate new JWT tokens
            val newAccessToken = jwtTokenManager.generateAccessToken(user)
            val newRefreshTokenEntity = refreshTokenService.createRefreshToken(user)
            
            // Revoke the old refresh token for security
            refreshTokenService.revokeToken(refreshToken)
            logger.debug("Old refresh token revoked for user: {}", user.id)
            
            // Calculate token expiration in seconds
            val expiresIn = jwtTokenManager.getTokenExpiration(newAccessToken)?.let { expiration ->
                (expiration - System.currentTimeMillis()) / 1000
            } ?: 900L // Default 15 minutes
            
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
     * Invalidates the provided refresh token to prevent further use
     * 
     * @param refreshToken The refresh token to revoke
     * @throws LogoutException if logout process fails
     */
    fun logout(refreshToken: String) {
        logger.info("Starting user logout")
        
        try {
            // Try to get user info for logging before revoking token
            val refreshTokenEntity = refreshTokenService.validateToken(refreshToken)
            val userId = refreshTokenEntity.map { it.user.id.toString() }.orElse(null)
            
            SecurityLogger.logLogoutAttempt(userId)
            
            // Revoke the refresh token
            val revoked = refreshTokenService.revokeToken(refreshToken)
            
            if (revoked) {
                SecurityLogger.logLogoutSuccess(userId)
            } else {
                logger.warn("Logout attempted with non-existent refresh token")
                // Don't throw exception for non-existent tokens to prevent information disclosure
                // Client should treat this as successful logout
                SecurityLogger.logLogoutSuccess(null)
            }
            
        } catch (ex: Exception) {
            SecurityLogger.logSecurityError("LOGOUT_ERROR", ex.message ?: "Unknown error")
            throw LogoutException("Logout failed: ${ex.message}", ex)
        }
    }
    
    /**
     * Logout user from all devices by revoking all their refresh tokens
     * Invalidates all refresh tokens for the user identified by the provided token
     * 
     * @param refreshToken Any valid refresh token for the user
     * @throws LogoutException if logout process fails
     */
    fun logoutFromAllDevices(refreshToken: String) {
        logger.info("Starting logout from all devices")
        
        try {
            // First validate the refresh token to get the user
            val refreshTokenEntity = refreshTokenService.validateToken(refreshToken)
                .orElseThrow { 
                    SecurityLogger.logSecurityError("LOGOUT_ALL_DEVICES_ERROR", "Invalid refresh token provided")
                    LogoutException("Invalid refresh token")
                }
            
            val user = refreshTokenEntity.user
            SecurityLogger.logLogoutAttempt(user.id.toString())
            
            // Revoke all refresh tokens for this user
            val revokedCount = refreshTokenService.revokeAllTokensForUser(user)
            
            logger.info("Logout from all devices successful - revoked {} tokens for user: {}", 
                       revokedCount, user.email)
            SecurityLogger.logLogoutSuccess(user.id.toString())
            
        } catch (ex: LogoutException) {
            SecurityLogger.logSecurityError("LOGOUT_ALL_DEVICES_ERROR", ex.message ?: "Logout exception")
            throw ex
        } catch (ex: Exception) {
            SecurityLogger.logSecurityError("LOGOUT_ALL_DEVICES_ERROR", "Unexpected error: ${ex.message}")
            throw LogoutException("Logout from all devices failed: ${ex.message}", ex)
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