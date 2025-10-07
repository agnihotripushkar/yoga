package com.devpush.yoga.features.classes.dto

import java.time.LocalDateTime

data class FavoriteResponse(
    val success: Boolean,
    val message: String,
    val isFavorite: Boolean
)

data class FavoriteClassResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val durationMinutes: Int,
    val difficultyLevel: String,
    val instructor: String?,
    val thumbnailUrl: String?,
    val favoritedAt: LocalDateTime
)