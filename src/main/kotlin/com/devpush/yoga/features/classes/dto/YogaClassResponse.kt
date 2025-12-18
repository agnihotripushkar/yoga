package com.devpush.yoga.features.classes.dto

import com.devpush.yoga.entity.DifficultyLevel
import java.time.LocalDateTime

import java.util.UUID

data class YogaClassResponse(
    val id: UUID,
    val title: String,
    val description: String?,
    val durationMinutes: Int,
    val difficultyLevel: DifficultyLevel,
    val instructor: String?,
    val videoUrl: String,
    val thumbnailUrl: String?,
    val isYoutube: Boolean,
    val tags: Set<String>,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)