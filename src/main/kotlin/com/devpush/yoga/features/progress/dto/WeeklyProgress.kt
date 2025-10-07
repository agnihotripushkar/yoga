package com.devpush.yoga.features.progress.dto

import java.time.LocalDate

data class WeeklyProgress(
    val weekStartDate: LocalDate,
    val weekEndDate: LocalDate,
    val sessions: Int = 0,
    val totalDurationMinutes: Int = 0,
    val totalCaloriesBurned: Int = 0,
    val averageSessionDuration: Double = 0.0,
    val daysActive: Int = 0,
    val dailyBreakdown: List<DailyProgress> = emptyList()
)

data class DailyProgress(
    val date: LocalDate,
    val sessions: Int = 0,
    val durationMinutes: Int = 0,
    val caloriesBurned: Int = 0
)