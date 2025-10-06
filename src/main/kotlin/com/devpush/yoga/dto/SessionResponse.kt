package com.devpush.yoga.dto

import java.time.LocalDateTime

data class SessionResponse(
    val id: Long,
    val durationMinutes: Int,
    val caloriesBurned: Int?,
    val completedAt: LocalDateTime,
    val notes: String?,
    val yogaClass: SessionYogaClassInfo?
)

data class SessionYogaClassInfo(
    val id: Long,
    val title: String,
    val instructor: String?,
    val difficultyLevel: String
)