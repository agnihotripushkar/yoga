package com.devpush.yoga.entity

import jakarta.persistence.*
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "yoga_sessions",
    indexes = [
        Index(name = "idx_yoga_sessions_user_id", columnList = "user_id"),
        Index(name = "idx_yoga_sessions_completed_at", columnList = "completed_at"),
        Index(name = "idx_yoga_sessions_user_completed", columnList = "user_id, completed_at")
    ]
)
data class YogaSession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    val user: User,
    
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = YogaClass::class)
    @JoinColumn(name = "class_id")
    val yogaClass: YogaClass? = null,
    
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Column(name = "duration_minutes", nullable = false)
    val durationMinutes: Int,
    
    @Min(value = 0, message = "Calories burned cannot be negative")
    @Column(name = "calories_burned")
    val caloriesBurned: Int? = null,
    
    @CreationTimestamp
    @Column(name = "completed_at", nullable = false, updatable = false)
    val completedAt: LocalDateTime? = null,
    
    @Column(length = 500)
    val notes: String? = null
) {
    // No-arg constructor for JPA
    constructor() : this(
        user = User(),
        durationMinutes = 0
    )
}