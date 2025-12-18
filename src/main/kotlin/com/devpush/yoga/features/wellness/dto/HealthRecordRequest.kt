package com.devpush.yoga.features.wellness.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class HealthRecordRequest(
    val date: LocalDate? = null, // Defaults to today if null
    
    @field:Min(value = 1, message = "Weight must be positive")
    val weight: Float? = null,
    
    @field:Min(value = 30, message = "Heart rate seems too low")
    @field:Max(value = 220, message = "Heart rate seems too high")
    val heartRate: Int? = null,
    
    @field:Min(value = 0, message = "Hydration cannot be negative")
    val hydration: Int? = null, // in ml
    
    @field:Min(value = 0, message = "Sleep hours cannot be negative")
    @field:Max(value = 24, message = "Sleep hours cannot exceed 24")
    val sleepHours: Float? = null,
    
    @field:Min(value = 1, message = "Mood score must be between 1 and 10")
    @field:Max(value = 10, message = "Mood score must be between 1 and 10")
    val moodScore: Int? = null,
    
    @field:Min(value = 1, message = "Stress level must be between 1 and 10")
    @field:Max(value = 10, message = "Stress level must be between 1 and 10")
    val stressLevel: Int? = null,
    
    @field:Size(max = 500, message = "Notes cannot exceed 500 characters")
    val notes: String? = null
)
