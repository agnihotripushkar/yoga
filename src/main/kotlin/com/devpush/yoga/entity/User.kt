package com.devpush.yoga.entity

import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["provider", "provider_id"])
    ]
)
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    
    @Email
    @NotBlank
    @Column(nullable = false)
    var email: String,
    
    // Mapped to display_name in DB as per PRD
    @Column(name = "display_name")
    var displayName: String? = null,

    // Keeping name as alias or legacy if needed, but mapped to name column for now if not display_name
    // PRD only mentions display_name. I'll map 'name' to 'display_name' or keep both?
    // Existing code uses 'name'. I will deprecate 'name' and use 'displayName' as primary.
    // Or simpler: Rename 'name' to 'displayName' if I can refactor all usages. 
    // For now I will add displayName and sync them or just use displayName.
    // Let's use displayName.
    @Transient
    var name: String? = null, 
    
    @Column(name = "profile_picture", length = 500)
    var avatarUrl: String? = null,

    // Legacy mapped field, alias to avatarUrl for compatibility
    @Transient
    var profilePicture: String? = null,
    
    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false)
    var provider: OAuthProvider,
    
    @NotBlank
    @Column(name = "provider_id", nullable = false)
    var providerId: String,
    
    // New PRD fields
    var sex: String? = null,
    var height: Float? = null,
    var weight: Float? = null,
    var level: Int? = null, // Experience level 1-5
    
    @Column(name = "total_minutes")
    var totalMinutes: Long = 0,

    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var preferences: UserPreferences? = null,
    
    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    @Column(length = 500)
    var bio: String? = null,
    
    // Deprecate logic or remove
    @Enumerated(EnumType.STRING)
    @Column(name = "fitness_level")
    var fitnessLevel: FitnessLevel? = null,
    
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
        fitnessLevel = null
    )

    // Helper for name/displayName backward compatibility
    @PostLoad
    fun syncFields() {
        this.name = this.displayName
        this.profilePicture = this.avatarUrl
    }

    @PrePersist
    @PreUpdate
    fun syncToDb() {
        if (this.displayName == null && this.name != null) {
            this.displayName = this.name
        }
        if (this.avatarUrl == null && this.profilePicture != null) {
            this.avatarUrl = this.profilePicture
        }
    }
}