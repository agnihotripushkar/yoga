package com.devpush.yoga.controller

import com.devpush.yoga.dto.ProfilePictureResponse
import com.devpush.yoga.dto.ProfileUpdateRequest
import com.devpush.yoga.dto.UserProfile
import com.devpush.yoga.service.AuthenticationException
import com.devpush.yoga.service.JwtTokenManager
import com.devpush.yoga.service.UserService
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
    private val jwtTokenManager: JwtTokenManager
) {
    
    private val logger = LoggerFactory.getLogger(ProfileController::class.java)
    
    /**
     * Update user profile information
     * Updates profile fields like name, bio, fitness level, and preferences
     * 
     * @param authorization Authorization header containing Bearer JWT token
     * @param profileUpdateRequest Profile update data
     * @return Updated UserProfile
     */
    @PutMapping
    fun updateProfile(
        @RequestHeader("Authorization") authorization: String,
        @Valid @RequestBody profileUpdateRequest: ProfileUpdateRequest
    ): ResponseEntity<UserProfile> {
        logger.info("Received profile update request")
        
        return try {
            val userId = extractAndValidateUserId(authorization)
            
            val updatedProfile = userService.updateProfile(userId, profileUpdateRequest)
            logger.info("Successfully updated profile for userId: {}", userId)
            
            ResponseEntity.ok(updatedProfile)
        } catch (ex: Exception) {
            logger.error("Profile update failed: {}", ex.message)
            when (ex) {
                is AuthenticationException -> throw ex
                is IllegalArgumentException -> throw AuthenticationException(ex.message ?: "Invalid request")
                else -> throw RuntimeException("Profile update failed", ex)
            }
        }
    }
    
    /**
     * Upload profile picture
     * Accepts image files (JPEG, PNG, WebP) up to 5MB
     * 
     * @param authorization Authorization header containing Bearer JWT token
     * @param file Profile picture file
     * @return ProfilePictureResponse with new picture URL
     */
    @PostMapping("/picture", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadProfilePicture(
        @RequestHeader("Authorization") authorization: String,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<ProfilePictureResponse> {
        logger.info("Received profile picture upload request")
        
        return try {
            val userId = extractAndValidateUserId(authorization)
            
            val profilePictureUrl = userService.uploadProfilePicture(userId, file)
            logger.info("Successfully uploaded profile picture for userId: {}", userId)
            
            val response = ProfilePictureResponse(
                profilePictureUrl = profilePictureUrl,
                message = "Profile picture uploaded successfully"
            )
            
            ResponseEntity.ok(response)
        } catch (ex: Exception) {
            logger.error("Profile picture upload failed: {}", ex.message)
            when (ex) {
                is AuthenticationException -> throw ex
                is IllegalArgumentException -> throw AuthenticationException(ex.message ?: "Invalid file")
                else -> throw RuntimeException("Profile picture upload failed", ex)
            }
        }
    }
    
    /**
     * Delete profile picture
     * Removes the user's current profile picture
     * 
     * @param authorization Authorization header containing Bearer JWT token
     * @return ProfilePictureResponse confirming deletion
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
            when (ex) {
                is AuthenticationException -> throw ex
                is IllegalArgumentException -> throw AuthenticationException(ex.message ?: "Invalid request")
                else -> throw RuntimeException("Profile picture deletion failed", ex)
            }
        }
    }
    
    /**
     * Extract and validate user ID from JWT token
     * 
     * @param authorization Authorization header
     * @return User ID from valid token
     * @throws AuthenticationException if token is invalid
     */
    private fun extractAndValidateUserId(authorization: String): Long {
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
        
        return userId
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