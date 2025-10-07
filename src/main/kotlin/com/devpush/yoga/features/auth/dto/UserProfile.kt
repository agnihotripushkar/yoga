package com.devpush.yoga.features.auth.dto

import com.devpush.yoga.entity.FitnessLevel
import com.devpush.yoga.entity.OAuthProvider
import java.time.LocalDateTime

data class UserProfile(
    val id: Long,
    val email: String,
    val name: String?,
    val profilePicture: String?,
    val provider: OAuthProvider,
    val bio: String? = null,
    val fitnessLevel: FitnessLevel? = null,
    val preferences: String? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)