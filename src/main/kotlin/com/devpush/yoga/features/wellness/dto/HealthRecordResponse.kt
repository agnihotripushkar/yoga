package com.devpush.yoga.features.wellness.dto

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class HealthRecordResponse(
    val id: UUID,
    val date: LocalDate,
    val weight: Float?,
    val heartRate: Int?,
    val hydration: Int?,
    val sleepHours: Float?,
    val moodScore: Int?,
    val stressLevel: Int?,
    val notes: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
