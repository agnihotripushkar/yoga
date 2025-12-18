package com.devpush.yoga.features.progress.dto

import java.time.LocalDateTime

import java.util.UUID

data class SessionResponse(
    val id: UUID,
    val durationMinutes: Int,
    val caloriesBurned: Int?,
    val classType: String?,
    val completed: Boolean,
    val completedAt: LocalDateTime,
    val notes: String?,
    val yogaClass: SessionYogaClassInfo?
)

data class SessionYogaClassInfo(
    val id: UUID,
    val title: String,
    val instructor: String?,
    val difficultyLevel: String
)