package com.devpush.yoga.features.profile.controller

import com.devpush.yoga.features.profile.dto.ProfilePictureResponse
import com.devpush.yoga.features.profile.dto.ProfileUpdateRequest
import com.devpush.yoga.dto.UserProfile
import com.devpush.yoga.features.auth.service.JwtTokenManager
import com.devpush.yoga.service.UserService
import com.devpush.yoga.service.RateLimitService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profile Management", description = "User profile management endpoints")
class ProfileController(
    private val userService: UserService,
    private val jwtTokenManager: JwtTokenManager,
    private val rateLimitService: RateLimitService
) {
    
    private val logger = LoggerFactory.getLogger(ProfileController::class.java)
    
    @Operation(
        summary = "Update User Profile",
        description = "Update user profile information including name, bio, and fitness level",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Profile updated successfully",
                content = [Content(
                    schema = Schema(implementation = UserProfile::class),
                    examples = [ExampleObject(
                        value = """
                        {
                          "id": 1,
                          "email": "user@example.com",
                          "name": "John Doe",
                          "bio": "Yoga enthusiast and beginner",
                          "fitnessLevel": "INTERMEDIATE",
                          "profilePicture": "https://example.com/profile.jpg"
                        }
                        """
                    )]
                )]
            ),
            ApiResponse(responseCode = "400", description = "Invalid profile data"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "429", description = "Rate limit exceeded")
        ]
    )
    @PutMapping
    fun updateProfile(
        @Parameter(description = "JWT access token", example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        @RequestHeader("Authorization") authorization: String,
        @Parameter(description = "Profile update data")
        @Valid @RequestBody profileUpdateRequest: ProfileUpdateRequest
    ): ResponseEntity<UserProfile> {
        logger.info("Received profile update request")
        
        return try {
            val userId = extractAndValidateUserId(authorization)
            
            // Apply rate limiting for profile updates
            rateLimitService.checkProfileUpdateRateLimit(userId.toString())
            
            val updatedProfile = userService.updateProfile(userId, profileUpdateRequest)
            logger.info("Successfully updated profile for userId: {}", userId)
            
            ResponseEntity.ok(updatedProfile)
        } catch (ex: Exception) {
            logger.error("Profile update failed: {}", ex.message)
            throw RuntimeException("Profile update failed", ex)
        }
    }
    
    @Operation(
        summary = "Upload Profile Picture",
        description = "Upload a new profile picture (JPEG, PNG, WebP formats, max 5MB)",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Profile picture uploaded successfully",
                content = [Content(schema = Schema(implementation = ProfilePictureResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "Invalid file format or size"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "413", description = "File too large"),
            ApiResponse(responseCode = "429", description = "Rate limit exceeded")
        ]
    )
    @PostMapping("/picture", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadProfilePicture(
        @Parameter(description = "JWT access token")
        @RequestHeader("Authorization") authorization: String,
        @Parameter(description = "Profile picture file (JPEG, PNG, WebP, max 5MB)")
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<ProfilePictureResponse> {
        logger.info("Received profile picture upload request")
        
        return try {
            val userId = extractAndValidateUserId(authorization)
            
            // Apply rate limiting for file uploads
            rateLimitService.checkFileUploadRateLimit(userId.toString())
            
            val profilePictureUrl = userService.uploadProfilePicture(userId, file)
            logger.info("Successfully uploaded profile picture for userId: {}", userId)
            
            val response = ProfilePictureResponse(
                profilePictureUrl = profilePictureUrl,
                message = "Profile picture uploaded successfully"
            )
            
            ResponseEntity.ok(response)
        } catch (ex: Exception) {
            logger.error("Profile picture upload failed: {}", ex.message)
            throw RuntimeException("Profile picture upload failed", ex)
        }
    }
    
    @Operation(
        summary = "Delete Profile Picture",
        description = "Remove the user's current profile picture",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Profile picture deleted successfully",
                content = [Content(schema = Schema(implementation = ProfilePictureResponse::class))]
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    @DeleteMapping("/picture")
    fun deleteProfilePicture(
        @Parameter(description = "JWT access token")
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<ProfilePictureResponse> {
        logger.info("Received profile picture deletion request")
        
        return try {
            val userId = extractAndValidateUserId(authorization)
            
            val deleted = userService.deleteProfilePicture(userId)
            logger.info("Profile picture deletion result for userId {}: {}", userId, deleted)
            
            val message = if (deleted) {
                "Profile picture deleted successfully"
            } else {
                "No profile picture to delete"
            }
            
            val response = ProfilePictureResponse(
                profilePictureUrl = null,
                message = message
            )
            
            ResponseEntity.ok(response)
        } catch (ex: Exception) {
            logger.error("Profile picture deletion failed: {}", ex.message)
            throw RuntimeException("Profile picture deletion failed", ex)
        }
    }
    
    /**
     * Extract and validate user ID from JWT token
     */
    private fun extractAndValidateUserId(authorization: String): Long {
        val token = extractTokenFromHeader(authorization)
            ?: throw RuntimeException("Invalid authorization header format")
        
        val userId = jwtTokenManager.getUserIdFromToken(token)
            ?: throw RuntimeException("Invalid or expired access token")
        
        if (!jwtTokenManager.isAccessToken(token)) {
            throw RuntimeException("Invalid token type. Access token required")
        }
        
        return userId
    }
    
    /**
     * Extract JWT token from Authorization header
     */
    private fun extractTokenFromHeader(authorization: String): String? {
        return if (authorization.startsWith("Bearer ", ignoreCase = true)) {
            authorization.substring(7)
        } else {
            null
        }
    }
}