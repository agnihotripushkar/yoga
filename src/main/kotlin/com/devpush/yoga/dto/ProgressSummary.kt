package com.devpush.yoga.dto

import java.time.LocalDateTime

data class ProgressSummary(
    val totalSessions: Int = 0,
    val totalDurationMinutes: Int = 0,
    val totalCaloriesBurned: Int = 0,
    val averageSessionDuration: Double = 0.0,
    val averageCaloriesPerSession: Double = 0.0,
    val firstSessionDate: LocalDateTime? = null,
    val lastSessionDate: LocalDateTime? = null,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
)