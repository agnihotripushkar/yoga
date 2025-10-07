package com.devpush.yoga.features.auth.controller

import com.devpush.yoga.features.auth.dto.*
import com.devpush.yoga.features.auth.service.AuthService
import com.devpush.yoga.features.auth.service.AuthenticationException
import com.devpush.yoga.features.auth.service.JwtTokenManager
import com.devpush.yoga.features.auth.service.LogoutException
import com.devpush.yoga.features.auth.service.TokenRefreshException
import com.devpush.yoga.service.RateLimitService
import com.devpush.yoga.service.UserService
import com.devpush.yoga.util.ClientIpExtractor
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
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
     */
    @PostMapping("/google/login")
    fun googleLogin(
        @Valid @RequestBody request: GoogleLoginRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<AuthResponse> {
        val clientIp = ClientIpExtractor.getClientIp(httpRequest)
        logger.info("Received Google login request from IP: {}", clientIp)

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
     */
    @PostMapping("/apple/login")
    fun appleLogin(
        @Valid @RequestBody request: AppleLoginRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<AuthResponse> {
        val clientIp = ClientIpExtractor.getClientIp(httpRequest)
        logger.info("Received Apple login request from IP: {}", clientIp)

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
     */
    @PostMapping("/refresh")
    fun refreshToken(
        @Valid @RequestBody request: RefreshTokenRequest
    ): ResponseEntity<AuthResponse> {
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
     */
    @GetMapping("/profile")
    fun getUserProfile(
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<UserProfile> {
        logger.info("Received get user profile request")

        return try {
            val token = extractTokenFromHeader(authorization)
                ?: throw AuthenticationException("Invalid authorization header format")

            val userId = jwtTokenManager.getUserIdFromToken(token)
                ?: throw AuthenticationException("Invalid or expired access token")

            if (!jwtTokenManager.isAccessToken(token)) {
                throw AuthenticationException("Invalid token type. Access token required")
            }

            val userProfile = userService.getUserProfile(userId).orElseThrow {
                AuthenticationException("User not found")
            }

            logger.info("Successfully retrieved profile for user: {}", userProfile.email)
            ResponseEntity.ok(userProfile)
        } catch (ex: AuthenticationException) {
            logger.error("Get user profile failed: {}", ex.message)
            throw ex
        }
    }

    private fun extractTokenFromHeader(authorization: String): String? {
        return if (authorization.startsWith("Bearer ", ignoreCase = true)) {
            authorization.substring(7)
        } else {
            null
        }
    }
}