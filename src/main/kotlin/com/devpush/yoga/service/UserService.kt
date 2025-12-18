package com.devpush.yoga.service

import com.devpush.yoga.features.auth.dto.OAuthUserInfo
import com.devpush.yoga.features.profile.dto.ProfileUpdateRequest
import com.devpush.yoga.features.auth.dto.UserProfile
import com.devpush.yoga.entity.OAuthProvider
import com.devpush.yoga.entity.User
import com.devpush.yoga.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository
) {
    
    private val logger = LoggerFactory.getLogger(UserService::class.java)
    
    /**
     * Find user by OAuth provider and provider ID
     * Used during OAuth authentication to check if user exists
     */
    fun findByProviderAndProviderId(provider: OAuthProvider, providerId: String): Optional<User> {
        logger.debug("Finding user by provider: {} and providerId: {}", provider, providerId)
        return userRepository.findByProviderAndProviderId(provider, providerId)
    }
    
    /**
     * Find user by email address
     * Used for user lookup and profile management
     */
    fun findByEmail(email: String): Optional<User> {
        logger.debug("Finding user by email: {}", email)
        return userRepository.findByEmail(email)
    }
    
    /**
     * Find user by ID
     * Used for profile retrieval with JWT authentication
     */
    fun findById(id: UUID): Optional<User> {
        logger.debug("Finding user by id: {}", id)
        return userRepository.findById(id)
    }
    
    /**
     * Create a new user from OAuth user information
     * Used when a user signs in for the first time
     */
    fun createUser(oAuthUserInfo: OAuthUserInfo): User {
        logger.info("Creating new user with provider: {} and email: {}", 
                   oAuthUserInfo.provider, oAuthUserInfo.email)
        
        val user = User(
            email = oAuthUserInfo.email,
            name = oAuthUserInfo.name,
            profilePicture = oAuthUserInfo.profilePicture,
            provider = oAuthUserInfo.provider,
            providerId = oAuthUserInfo.providerId
        )
        
        val savedUser = userRepository.save(user)
        logger.info("Successfully created user with id: {}", savedUser.id)
        return savedUser
    }
    
    /**
     * Update existing user with new OAuth information
     * Used when user signs in and profile information may have changed
     */
    fun updateUser(existingUser: User, oAuthUserInfo: OAuthUserInfo): User {
        logger.info("Updating user with id: {} from provider: {}", 
                   existingUser.id, oAuthUserInfo.provider)
        
        // Update user information with latest from OAuth provider
        existingUser.email = oAuthUserInfo.email
        existingUser.name = oAuthUserInfo.name
        existingUser.displayName = oAuthUserInfo.name // Ensure displayName is synced
        existingUser.profilePicture = oAuthUserInfo.profilePicture
        existingUser.avatarUrl = oAuthUserInfo.profilePicture // Ensure avatarUrl is synced
        
        val updatedUser = userRepository.save(existingUser)
        logger.info("Successfully updated user with id: {}", updatedUser.id)
        return updatedUser
    }
    
    /**
     * Create or update user based on OAuth information
     * Main method used during OAuth authentication flow
     */
    fun createOrUpdateUser(oAuthUserInfo: OAuthUserInfo): User {
        logger.debug("Creating or updating user for provider: {} and providerId: {}", 
                    oAuthUserInfo.provider, oAuthUserInfo.providerId)
        
        val existingUser = findByProviderAndProviderId(oAuthUserInfo.provider, oAuthUserInfo.providerId)
        
        return if (existingUser.isPresent) {
            updateUser(existingUser.get(), oAuthUserInfo)
        } else {
            createUser(oAuthUserInfo)
        }
    }
    
    /**
     * Convert User entity to UserProfile DTO
     * Used for API responses and profile information
     */
    fun toUserProfile(user: User): UserProfile {
        return UserProfile(
            id = user.id ?: throw IllegalStateException("User ID cannot be null"),
            email = user.email,
            name = user.name ?: user.displayName,
            displayName = user.displayName,
            profilePicture = user.profilePicture ?: user.avatarUrl,
            provider = user.provider,
            bio = user.bio,
            sex = user.sex,
            height = user.height,
            weight = user.weight,
            level = user.level,
            totalMinutes = user.totalMinutes,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt
        )
    }
    
    /**
     * Check if user exists by provider and provider ID
     * Used for quick existence checks during authentication
     */
    fun existsByProviderAndProviderId(provider: OAuthProvider, providerId: String): Boolean {
        logger.debug("Checking if user exists for provider: {} and providerId: {}", provider, providerId)
        return userRepository.existsByProviderAndProviderId(provider, providerId)
    }
    
    /**
     * Get user profile by user ID
     * Used for authenticated profile retrieval
     */
    fun getUserProfile(userId: UUID): Optional<UserProfile> {
        logger.debug("Getting user profile for userId: {}", userId)
        return findById(userId).map { toUserProfile(it) }
    }
    
    /**
     * Update user profile with new information
     * Used for profile management functionality
     */
    fun updateProfile(userId: UUID, profileUpdateRequest: ProfileUpdateRequest): UserProfile {
        logger.info("Updating profile for userId: {}", userId)
        
        val user = findById(userId).orElseThrow { 
            IllegalArgumentException("User not found with id: $userId") 
        }
        
        // Update profile fields with validation and sanitization
        profileUpdateRequest.displayName?.let { 
            user.displayName = sanitizeInput(it.trim())
            user.name = user.displayName
        } ?: profileUpdateRequest.name?.let {
            user.displayName = sanitizeInput(it.trim())
            user.name = user.displayName
        }
        
        profileUpdateRequest.bio?.let { 
            user.bio = sanitizeInput(it.trim())
        }
        
        profileUpdateRequest.sex?.let { user.sex = sanitizeInput(it.trim()) }
        profileUpdateRequest.height?.let { user.height = it }
        profileUpdateRequest.weight?.let { user.weight = it }
        profileUpdateRequest.level?.let { user.level = it }
        
        profileUpdateRequest.fitnessLevel?.let { 
             user.fitnessLevel = it
        }
        
        profileUpdateRequest.preferences?.let { 
            // user.preferences is complex object now, handling simple string update if any
        }
        
        val updatedUser = userRepository.save(user)
        logger.info("Successfully updated profile for userId: {}", userId)
        
        return toUserProfile(updatedUser)
    }
    
    /**
     * Upload and save profile picture for user
     * Used for profile picture management
     */
    fun uploadProfilePicture(userId: UUID, file: MultipartFile): String {
        logger.info("Uploading profile picture for userId: {}", userId)
        
        val user = findById(userId).orElseThrow { 
            IllegalArgumentException("User not found with id: $userId") 
        }
        
        // Validate file
        validateProfilePicture(file)
        
        // Delete existing profile picture if it exists
        user.profilePicture?.let { deleteProfilePictureFile(it) }
        
        // Save new profile picture
        val fileName = generateProfilePictureFileName(userId, file)
        val filePath = saveProfilePictureFile(file, fileName)
        
        // Update user entity
        user.profilePicture = filePath
        user.avatarUrl = filePath
        userRepository.save(user)
        
        logger.info("Successfully uploaded profile picture for userId: {}", userId)
        return filePath
    }
    
    /**
     * Delete user's profile picture
     * Used for profile picture removal
     */
    fun deleteProfilePicture(userId: UUID): Boolean {
        logger.info("Deleting profile picture for userId: {}", userId)
        
        val user = findById(userId).orElseThrow { 
            IllegalArgumentException("User not found with id: $userId") 
        }
        
        val profilePicture = user.profilePicture
        if (profilePicture != null) {
            deleteProfilePictureFile(profilePicture)
            user.profilePicture = null
            user.avatarUrl = null
            userRepository.save(user)
            logger.info("Successfully deleted profile picture for userId: {}", userId)
            return true
        }
        
        logger.debug("No profile picture to delete for userId: {}", userId)
        return false
    }
    
    /**
     * Validate uploaded profile picture file
     */
    private fun validateProfilePicture(file: MultipartFile) {
        // Check if file is empty
        if (file.isEmpty) {
            throw IllegalArgumentException("Profile picture file cannot be empty")
        }
        
        // Check file size (5MB limit)
        val maxSize = 5 * 1024 * 1024 // 5MB in bytes
        if (file.size > maxSize) {
            throw IllegalArgumentException("Profile picture file size cannot exceed 5MB")
        }
        
        // Check file type
        val allowedTypes = setOf("image/jpeg", "image/png", "image/webp")
        val contentType: String? = file.contentType
        if (contentType == null || contentType !in allowedTypes) {
            throw IllegalArgumentException("Profile picture must be JPEG, PNG, or WebP format")
        }
        
        // Validate file extension
        val originalFilename = file.originalFilename ?: ""
        val extension = if (originalFilename.contains('.')) {
            originalFilename.substringAfterLast('.').lowercase()
        } else {
            ""
        }
        val allowedExtensions = setOf("jpg", "jpeg", "png", "webp")
        if (extension.isEmpty() || extension !in allowedExtensions) {
            throw IllegalArgumentException("Invalid file extension. Allowed: jpg, jpeg, png, webp")
        }
    }
    
    /**
     * Generate unique filename for profile picture
     */
    private fun generateProfilePictureFileName(userId: UUID, file: MultipartFile): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val originalFilename = file.originalFilename ?: "profile.jpg"
        val extension = if (originalFilename.contains('.')) {
            originalFilename.substringAfterLast('.')
        } else {
            "jpg"
        }
        return "profile_${userId}_${timestamp}.${extension}"
    }
    
    /**
     * Save profile picture file to storage
     */
    private fun saveProfilePictureFile(file: MultipartFile, fileName: String): String {
        try {
            // Create uploads directory if it doesn't exist
            val uploadDir = Paths.get("uploads", "profiles")
            Files.createDirectories(uploadDir)
            
            // Save file
            val filePath = uploadDir.resolve(fileName)
            Files.copy(file.inputStream, filePath, StandardCopyOption.REPLACE_EXISTING)
            
            // Return relative path for storage in database
            return "uploads/profiles/$fileName"
        } catch (e: IOException) {
            logger.error("Failed to save profile picture file: {}", fileName, e)
            throw RuntimeException("Failed to save profile picture", e)
        }
    }
    
    /**
     * Delete profile picture file from storage
     */
    private fun deleteProfilePictureFile(filePath: String) {
        try {
            val path = Paths.get(filePath)
            if (Files.exists(path)) {
                Files.delete(path)
                logger.debug("Deleted profile picture file: {}", filePath)
            }
        } catch (e: IOException) {
            logger.warn("Failed to delete profile picture file: {}", filePath, e)
        }
    }
    
    /**
     * Sanitize user input to prevent XSS and other security issues
     */
    private fun sanitizeInput(input: String): String {
        return input
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;")
    }
}