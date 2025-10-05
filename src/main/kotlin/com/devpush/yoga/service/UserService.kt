package com.devpush.yoga.service

import com.devpush.yoga.dto.OAuthUserInfo
import com.devpush.yoga.dto.UserProfile
import com.devpush.yoga.entity.OAuthProvider
import com.devpush.yoga.entity.User
import com.devpush.yoga.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
    fun findById(id: Long): Optional<User> {
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
        existingUser.profilePicture = oAuthUserInfo.profilePicture
        
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
            name = user.name,
            profilePicture = user.profilePicture,
            provider = user.provider
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
    fun getUserProfile(userId: Long): Optional<UserProfile> {
        logger.debug("Getting user profile for userId: {}", userId)
        return findById(userId).map { toUserProfile(it) }
    }
}