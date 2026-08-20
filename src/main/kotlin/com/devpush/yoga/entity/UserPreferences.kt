package com.devpush.yoga.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "user_preferences")
data class UserPreferences(
    @Id
    @Column(name = "user_id")
    val userId: UUID? = null,

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    val user: User? = null,

    @Column(name = "notifications_enabled")
    var notificationsEnabled: Boolean = true,

    @Column(name = "preferred_session_length")
    var preferredSessionLength: Int = 30,

    @Column(name = "difficulty_level")
    @Enumerated(EnumType.STRING)
    var difficultyLevel: DifficultyLevel = DifficultyLevel.BEGINNER,

    var language: String = "en",

    @Column(name = "auto_play_next")
    var autoPlayNext: Boolean = false,

    @Column(name = "download_quality")
    var downloadQuality: String = "HD"
)
