package com.devpush.yoga.controller

import com.devpush.yoga.dto.*
import com.devpush.yoga.service.AuthService
import com.devpush.yoga.service.UserService
import com.devpush.yoga.service.JwtTokenManager
import com.devpush.yoga.service.RateLimitService
import com.devpush.yoga.service.AuthenticationException
import com.devpush.yoga.service.TokenRefreshException
import com.devpush.yoga.service.LogoutException
import com.devpush.yoga.util.ClientIpExtractor
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val userService: UserService,
    private val jwtTokenManager: JwtTokenManager,
    private val rateLimitService: RateLimitService
) {
    
    private val logger = LoggerFactory.getLogger(AuthController::class.java)
    
    /**
     * Google OAuth login endpoint
     * Validates Google ID token and returns JWT tokens
     * 
     * @param request GoogleLoginRequest containing the Google ID token
     * @param httpRequest HttpServletRequest for extracting client IP
     * @return AuthResponse with JWT tokens and user profile
     */
    @PostMapping("/google/login")
    fun googleLogin(
        @Valid @RequestBody request: GoogleLoginRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<AuthResponse> {
        val clientIp = ClientIpExtractor.getClientIp(httpRequest)
        logger.info("Received Google login request from IP: {}", clientIp)
        
        // Apply rate limiting
        rateLimitService.checkAuthenticationRateLimit(clientIp)
        
        return try {
            val authResponse = authService.authenticateWithGoogle(request.idToken)
            logger.info("Google login successful for user: {}", authResponse.user.email)
            ResponseEntity.ok(authResponse)
        } catch (ex: AuthenticationException) {
            logger.error("Google login failed: {}", ex.message)
            throw ex
        }
    }
    
    /**
     * Apple OAuth login endpoint
     * Validates Apple ID token and returns JWT tokens
     * 
     * @param request AppleLoginRequest containing the Apple ID token
     * @param httpRequest HttpServletRequest for extracting client IP
     * @return AuthResponse with JWT tokens and user profile
     */
    @PostMapping("/apple/login")
    fun appleLogin(
        @Valid @RequestBody request: AppleLoginRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<AuthResponse> {
        val clientIp = ClientIpExtractor.getClientIp(httpRequest)
        logger.info("Received Apple login request from IP: {}", clientIp)
        
        // Apply rate limiting
        rateLimitService.checkAuthenticationRateLimit(clientIp)
        
        return try {
            val authResponse = authService.authenticateWithApple(request.idToken)
            logger.info("Apple login successful for user: {}", authResponse.user.email)
            ResponseEntity.ok(authResponse)
        } catch (ex: AuthenticationException) {
            logger.error("Apple login failed: {}", ex.message)
            throw ex
        }
    }
    
    /**
     * Token refresh endpoint
     * Validates refresh token and returns new JWT tokens
     * 
     * @param request RefreshTokenRequest containing the refresh token
     * @return AuthResponse with new JWT tokens and user profile
     */
    @PostMapping("/refresh")
    fun refreshToken(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<AuthResponse> {
        logger.info("Received token refresh request")
        
        return try {
            val authResponse = authService.refreshTokens(request.refreshToken)
            logger.info("Token refresh successful for user: {}", authResponse.user.email)
            ResponseEntity.ok(authResponse)
        } catch (ex: TokenRefreshException) {
            logger.error("Token refresh failed: {}", ex.message)
            throw ex
        }
    }
    
    /**
     * Logout endpoint
     * Revokes the provided refresh token
     * 
     * @param request RefreshTokenRequest containing the refresh token to revoke
     * @return Empty response with 204 No Content status
     */
    @PostMapping("/logout")
    fun logout(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<Void> {
        logger.info("Received logout request")
        
        return try {
            authService.logout(request.refreshToken)
            logger.info("Logout successful")
            ResponseEntity.noContent().build()
        } catch (ex: LogoutException) {
            logger.error("Logout failed: {}", ex.message)
            throw ex
        }
    }
    
    /**
     * Get user profile endpoint
     * Returns user profile information for authenticated users
     * 
     * @param authorization Authorization header containing Bearer JWT token
     * @return UserProfile with user information
     */
    @GetMapping("/profile")
    fun getUserProfile(@RequestHeader("Authorization") authorization: String): ResponseEntity<UserProfile> {
        logger.info("Received get user profile request")
        
        return try {
            // Extract JWT token from Authorization header
            val token = extractTokenFromHeader(authorization)
                ?: throw AuthenticationException("Invalid authorization header format")
            
            // Validate token and extract user ID
            val userId = jwtTokenManager.getUserIdFromToken(token)
                ?: throw AuthenticationException("Invalid or expired access token")
            
            // Verify it's an access token
            if (!jwtTokenManager.isAccessToken(token)) {
                throw AuthenticationException("Invalid token type. Access token required")
            }
            
            // Get user profile
            val userProfile = userService.getUserProfile(userId)
                .orElseThrow { AuthenticationException("User not found") }
            
            logger.info("Successfully retrieved profile for user: {}", userProfile.email)
            ResponseEntity.ok(userProfile)
            
        } catch (ex: AuthenticationException) {
            logger.error("Get user profile failed: {}", ex.message)
            throw ex
        }
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