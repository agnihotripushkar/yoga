package com.devpush.yoga.features.classes.dto

import com.devpush.yoga.entity.DifficultyLevel
import java.time.LocalDateTime

data class YogaClassResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val durationMinutes: Int,
    val difficultyLevel: DifficultyLevel,
    val instructor: String?,
    val videoUrl: String,
    val thumbnailUrl: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)