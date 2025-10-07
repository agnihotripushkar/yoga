package com.devpush.yoga.repository

import com.devpush.yoga.entity.FitnessLevel
import com.devpush.yoga.entity.OAuthProvider
import com.devpush.yoga.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*
import jakarta.persistence.QueryHint

@Repository
interface UserRepository : JpaRepository<User, Long> {
    
    // Find user by OAuth provider and provider ID (optimized with composite index)
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun findByProviderAndProviderId(provider: OAuthProvider, providerId: String): Optional<User>
    
    // Find user by email address (optimized with email index)
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun findByEmail(email: String): Optional<User>
    
    // Check if user exists with given provider and provider ID (optimized existence check)
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun existsByProviderAndProviderId(provider: OAuthProvider, providerId: String): Boolean
    
    // Find all users by OAuth provider (with pagination for large datasets)
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun findByProvider(provider: OAuthProvider, pageable: Pageable): Page<User>
    
    // Keep original method for backward compatibility
    fun findByProvider(provider: OAuthProvider): List<User>
    
    // Custom query to find user by provider and provider ID with email (optimized)
    @Query("SELECT u FROM User u WHERE u.provider = :provider AND u.providerId = :providerId AND u.email = :email")
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun findByProviderAndProviderIdAndEmail(
        @Param("provider") provider: OAuthProvider,
        @Param("providerId") providerId: String,
        @Param("email") email: String
    ): Optional<User>
    
    // Find users by fitness level (optimized with fitness level index)
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun findByFitnessLevel(fitnessLevel: FitnessLevel, pageable: Pageable): Page<User>
    
    // Find users with complete profiles (have bio and fitness level)
    @Query("SELECT u FROM User u WHERE u.bio IS NOT NULL AND u.fitnessLevel IS NOT NULL ORDER BY u.updatedAt DESC")
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun findUsersWithCompleteProfiles(pageable: Pageable): Page<User>
    
    // Find recently updated profiles
    @Query("SELECT u FROM User u WHERE u.updatedAt >= :sinceDate ORDER BY u.updatedAt DESC")
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun findRecentlyUpdatedProfiles(@Param("sinceDate") sinceDate: LocalDateTime, pageable: Pageable): Page<User>
    
    // Count users by fitness level for analytics
    @Query("SELECT u.fitnessLevel, COUNT(u) FROM User u WHERE u.fitnessLevel IS NOT NULL GROUP BY u.fitnessLevel ORDER BY COUNT(u) DESC")
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun getUserCountsByFitnessLevel(): List<Array<Any>>
    
    // Count users by OAuth provider for analytics
    @Query("SELECT u.provider, COUNT(u) FROM User u GROUP BY u.provider ORDER BY COUNT(u) DESC")
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun getUserCountsByProvider(): List<Array<Any>>
    
    // Find active users (users with recent sessions)
    @Query("SELECT DISTINCT u FROM User u JOIN YogaSession s ON s.user = u WHERE s.completedAt >= :sinceDate ORDER BY u.name, u.email")
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun findActiveUsers(@Param("sinceDate") sinceDate: LocalDateTime, pageable: Pageable): Page<User>
    
    // Find users with profile pictures
    @Query("SELECT u FROM User u WHERE u.profilePicture IS NOT NULL ORDER BY u.updatedAt DESC")
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun findUsersWithProfilePictures(pageable: Pageable): Page<User>
    
    // Find users without profile pictures (for engagement campaigns)
    @Query("SELECT u FROM User u WHERE u.profilePicture IS NULL ORDER BY u.createdAt DESC")
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun findUsersWithoutProfilePictures(pageable: Pageable): Page<User>
    
    // User registration statistics by date
    @Query("SELECT DATE(u.createdAt), COUNT(u) FROM User u WHERE u.createdAt >= :sinceDate GROUP BY DATE(u.createdAt) ORDER BY DATE(u.createdAt) DESC")
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun getUserRegistrationStats(@Param("sinceDate") sinceDate: LocalDateTime): List<Array<Any>>
    
    // Search users by name or email (case-insensitive)
    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY u.name, u.email")
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun searchUsers(@Param("searchTerm") searchTerm: String, pageable: Pageable): Page<User>
}