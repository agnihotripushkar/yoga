package com.devpush.yoga.entity

import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["provider", "provider_id"])
    ]
)
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Email
    @NotBlank
    @Column(nullable = false)
    var email: String,
    
    var name: String? = null,
    
    @Column(name = "profile_picture", length = 500)
    var profilePicture: String? = null,
    
    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false)
    var provider: OAuthProvider,
    
    @NotBlank
    @Column(name = "provider_id", nullable = false)
    var providerId: String,
    
    // New profile fields
    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    @Column(length = 500)
    var bio: String? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "fitness_level")
    var fitnessLevel: FitnessLevel? = null,
    
    @Size(max = 1000, message = "Preferences cannot exceed 1000 characters")
    @Column(length = 1000)
    var preferences: String? = null,
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
) {
    // No-arg constructor for JPA
    constructor() : this(
        email = "",
        provider = OAuthProvider.GOOGLE,
        providerId = "",
        bio = null,
        fitnessLevel = null,
        preferences = null
    )
}