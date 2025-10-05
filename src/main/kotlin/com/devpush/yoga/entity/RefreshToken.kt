package com.devpush.yoga.entity

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "refresh_tokens")
data class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @NotBlank
    @Column(nullable = false, unique = true, length = 500)
    var token: String,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    var user: User,
    
    @Column(name = "expires_at", nullable = false)
    @NotNull
    var expiresAt: LocalDateTime,
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,
    
    @Column(nullable = false)
    var revoked: Boolean = false
) {
    // No-arg constructor for JPA
    constructor() : this(
        token = "",
        user = User(),
        expiresAt = LocalDateTime.now()
    )
    
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)
    
    fun isValid(): Boolean = !revoked && !isExpired()
}