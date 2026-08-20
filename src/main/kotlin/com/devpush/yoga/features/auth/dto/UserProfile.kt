package com.devpush.yoga.features.auth.dto

import com.devpush.yoga.entity.FitnessLevel
import com.devpush.yoga.entity.OAuthProvider
import java.time.LocalDateTime
import java.util.UUID

data class UserProfile(
    val id: UUID,
    val email: String,
    val name: String?,
    val displayName: String?,
    val profilePicture: String?,
    val provider: OAuthProvider,
    val bio: String? = null,
    val sex: String? = null,
    val height: Float? = null,
    val weight: Float? = null,
    val level: Int? = null,
    val totalMinutes: Long = 0,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)