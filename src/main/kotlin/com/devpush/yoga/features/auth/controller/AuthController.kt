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
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "OAuth authentication endpoints for Google and Apple Sign-In")
class AuthController(
    private val authService: AuthService,
    private val userService: UserService,
    private val jwtTokenManager: JwtTokenManager,
    private val rateLimitService: RateLimitService
) {

    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    @Operation(
        summary = "Google OAuth Login",
        description = "Authenticate user with Google ID token and return JWT tokens"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Authentication successful",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = AuthResponse::class),
                    examples = [ExampleObject(
                        name = "Success Response",
                        value = """
                        {
                          "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                          "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                          "user": {
                            "id": 1,
                            "email": "user@example.com",
                            "name": "John Doe",
                            "profilePicture": null,
                            "fitnessLevel": "BEGINNER"
                          }
                        }
                        """
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid Google ID token",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        value = """{"error": "INVALID_TOKEN", "message": "Invalid Google ID token"}"""
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "429",
                description = "Rate limit exceeded",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        value = """{"error": "RATE_LIMIT_EXCEEDED", "message": "Too many authentication attempts"}"""
                    )]
                )]
            )
        ]
    )
    @PostMapping("/google/login")
    fun googleLogin(
        @Parameter(description = "Google login request containing ID token")
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

    @Operation(
        summary = "Apple OAuth Login",
        description = "Authenticate user with Apple ID token and return JWT tokens"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Authentication successful",
                content = [Content(schema = Schema(implementation = AuthResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid Apple ID token"
            ),
            ApiResponse(
                responseCode = "429",
                description = "Rate limit exceeded"
            )
        ]
    )
    @PostMapping("/apple/login")
    fun appleLogin(
        @Parameter(description = "Apple login request containing ID token")
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

    @Operation(
        summary = "Refresh Access Token",
        description = "Generate new access and refresh tokens using a valid refresh token"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Token refresh successful",
                content = [Content(schema = Schema(implementation = AuthResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired refresh token"
            )
        ]
    )
    @PostMapping("/refresh")
    fun refreshToken(
        @Parameter(description = "Refresh token request")
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

    @Operation(
        summary = "Logout User",
        description = "Invalidate refresh token and log out the user"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Logout successful"
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid refresh token"
            )
        ]
    )
    @PostMapping("/logout")
    fun logout(
        @Parameter(description = "Refresh token to invalidate")
        @Valid @RequestBody request: RefreshTokenRequest
    ): ResponseEntity<Void> {
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

    @Operation(
        summary = "Get User Profile",
        description = "Retrieve the authenticated user's profile information",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Profile retrieved successfully",
                content = [Content(schema = Schema(implementation = UserProfile::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired access token"
            ),
            ApiResponse(
                responseCode = "404",
                description = "User not found"
            )
        ]
    )
    @GetMapping("/profile")
    fun getUserProfile(
        @Parameter(description = "JWT access token", example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
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