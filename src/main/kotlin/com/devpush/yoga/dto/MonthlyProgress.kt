package com.devpush.yoga.dto

import java.time.LocalDate

data class MonthlyProgress(
    val monthStartDate: LocalDate,
    val monthEndDate: LocalDate,
    val sessions: Int = 0,
    val totalDurationMinutes: Int = 0,
    val totalCaloriesBurned: Int = 0,
    val averageSessionDuration: Double = 0.0,
    val daysActive: Int = 0,
    val weeklyBreakdown: List<WeeklyProgressSummary> = emptyList()
)

data class WeeklyProgressSummary(
    val weekNumber: Int,
    val weekStartDate: LocalDate,
    val weekEndDate: LocalDate,
    val sessions: Int = 0,
    val durationMinutes: Int = 0,
    val caloriesBurned: Int = 0
)