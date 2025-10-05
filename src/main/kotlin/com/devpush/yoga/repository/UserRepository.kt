package com.devpush.yoga.repository

import com.devpush.yoga.entity.OAuthProvider
import com.devpush.yoga.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long> {
    
    /**
     * Find user by OAuth provider and provider ID
     * Used for OAuth authentication to check if user already exists
     */
    fun findByProviderAndProviderId(provider: OAuthProvider, providerId: String): Optional<User>
    
    /**
     * Find user by email address
     * Used for user lookup and profile management
     */
    fun findByEmail(email: String): Optional<User>
    
    /**
     * Check if user exists with given provider and provider ID
     * Used for quick existence checks during authentication
     */
    fun existsByProviderAndProviderId(provider: OAuthProvider, providerId: String): Boolean
    
    /**
     * Find all users by OAuth provider
     * Used for administrative purposes and analytics
     */
    fun findByProvider(provider: OAuthProvider): List<User>
    
    /**
     * Custom query to find user by provider and provider ID with email
     * Used for comprehensive user lookup during OAuth authentication
     */
    @Query("SELECT u FROM User u WHERE u.provider = :provider AND u.providerId = :providerId AND u.email = :email")
    fun findByProviderAndProviderIdAndEmail(
        @Param("provider") provider: OAuthProvider,
        @Param("providerId") providerId: String,
        @Param("email") email: String
    ): Optional<User>
}