package com.devpush.yoga.features.profile.controller

import com.devpush.yoga.features.profile.dto.ProfilePictureResponse
import com.devpush.yoga.features.profile.dto.ProfileUpdateRequest
import com.devpush.yoga.dto.UserProfile
import com.devpush.yoga.features.auth.service.JwtTokenManager
import com.devpush.yoga.service.UserService
import com.devpush.yoga.service.RateLimitService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/profile")
class ProfileController(
    private val userService: UserService,
    private val jwtTokenManager: JwtTokenManager,
    private val rateLimitService: RateLimitService
) {
    
    private val logger = LoggerFactory.getLogger(ProfileController::class.java)
    
    /**
     * Update user profile information
     */
    @PutMapping
    fun updateProfile(
        @RequestHeader("Authorization") authorization: String,
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
    
    /**
     * Upload profile picture
     */
    @PostMapping("/picture", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadProfilePicture(
        @RequestHeader("Authorization") authorization: String,
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
    
    /**
     * Delete profile picture
     */
    @DeleteMapping("/picture")
    fun deleteProfilePicture(
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