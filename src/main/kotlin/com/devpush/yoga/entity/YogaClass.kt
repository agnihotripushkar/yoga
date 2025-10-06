package com.devpush.yoga.entity

import jakarta.persistence.*
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "yoga_classes",
    indexes = [
        Index(name = "idx_yoga_classes_title", columnList = "title"),
        Index(name = "idx_yoga_classes_instructor", columnList = "instructor"),
        Index(name = "idx_yoga_classes_difficulty", columnList = "difficulty_level"),
        Index(name = "idx_yoga_classes_duration", columnList = "duration_minutes")
    ]
)
data class YogaClass(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    @Column(nullable = false, length = 200)
    val title: String,
    
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Column(length = 1000)
    val description: String? = null,
    
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Column(name = "duration_minutes", nullable = false)
    val durationMinutes: Int,
    
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Difficulty level is required")
    @Column(name = "difficulty_level", nullable = false)
    val difficultyLevel: DifficultyLevel,
    
    @Size(max = 100, message = "Instructor name cannot exceed 100 characters")
    @Column(length = 100)
    val instructor: String? = null,
    
    @NotBlank(message = "Video URL is required")
    @Pattern(
        regexp = "^https?://.*",
        message = "Video URL must be a valid HTTP or HTTPS URL"
    )
    @Column(name = "video_url", nullable = false, length = 500)
    val videoUrl: String,
    
    @Pattern(
        regexp = "^https?://.*",
        message = "Thumbnail URL must be a valid HTTP or HTTPS URL"
    )
    @Column(name = "thumbnail_url", length = 500)
    val thumbnailUrl: String? = null,
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null
) {
    // No-arg constructor for JPA
    constructor() : this(
        title = "",
        durationMinutes = 0,
        difficultyLevel = DifficultyLevel.BEGINNER,
        videoUrl = ""
    )
}