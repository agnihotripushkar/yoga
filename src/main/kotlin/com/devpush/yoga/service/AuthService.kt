package com.devpush.yoga.service

import com.devpush.yoga.dto.AuthResponse
import com.devpush.yoga.dto.UserProfile
import com.devpush.yoga.entity.User
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
            logger.debug("Successfully validated Google token for user: {}", oAuthUserInfo.email)
            
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
            
            logger.info("Google authentication successful for user: {}", user.email)
            
            return AuthResponse(
                accessToken = accessToken,
                refreshToken = refreshTokenEntity.token,
                expiresIn = expiresIn,
                user = userProfile
            )
            
        } catch (ex: GoogleTokenValidationException) {
            logger.error("Google token validation failed: {}", ex.message)
            throw AuthenticationException("Google authentication failed: ${ex.message}", ex)
        } catch (ex: Exception) {
            logger.error("Unexpected error during Google authentication", ex)
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
            logger.debug("Successfully validated Apple token for user: {}", oAuthUserInfo.email)
            
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
            
            logger.info("Apple authentication successful for user: {}", user.email)
            
            return AuthResponse(
                accessToken = accessToken,
                refreshToken = refreshTokenEntity.token,
                expiresIn = expiresIn,
                user = userProfile
            )
            
        } catch (ex: AppleTokenValidationException) {
            logger.error("Apple token validation failed: {}", ex.message)
            throw AuthenticationException("Apple authentication failed: ${ex.message}", ex)
        } catch (ex: Exception) {
            logger.error("Unexpected error during Apple authentication", ex)
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
                    logger.warn("Invalid or expired refresh token provided")
                    TokenRefreshException("Invalid or expired refresh token")
                }
            
            val user = refreshTokenEntity.user
            logger.debug("Refresh token validated for user: {}", user.id)
            
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
            
            logger.info("Token refresh successful for user: {}", user.email)
            
            return AuthResponse(
                accessToken = newAccessToken,
                refreshToken = newRefreshTokenEntity.token,
                expiresIn = expiresIn,
                user = userProfile
            )
            
        } catch (ex: TokenRefreshException) {
            logger.error("Token refresh failed: {}", ex.message)
            throw ex
        } catch (ex: Exception) {
            logger.error("Unexpected error during token refresh", ex)
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
            // Revoke the refresh token
            val revoked = refreshTokenService.revokeToken(refreshToken)
            
            if (revoked) {
                logger.info("User logout successful - refresh token revoked")
            } else {
                logger.warn("Logout attempted with non-existent refresh token")
                // Don't throw exception for non-existent tokens to prevent information disclosure
                // Client should treat this as successful logout
            }
            
        } catch (ex: Exception) {
            logger.error("Unexpected error during logout", ex)
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
                    logger.warn("Invalid refresh token provided for logout from all devices")
                    LogoutException("Invalid refresh token")
                }
            
            val user = refreshTokenEntity.user
            
            // Revoke all refresh tokens for this user
            val revokedCount = refreshTokenService.revokeAllTokensForUser(user)
            
            logger.info("Logout from all devices successful - revoked {} tokens for user: {}", 
                       revokedCount, user.email)
            
        } catch (ex: LogoutException) {
            logger.error("Logout from all devices failed: {}", ex.message)
            throw ex
        } catch (ex: Exception) {
            logger.error("Unexpected error during logout from all devices", ex)
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