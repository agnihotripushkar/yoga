package com.devpush.yoga.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class SessionRecordRequest(
    @field:NotNull(message = "Duration is required")
    @field:Min(value = 1, message = "Duration must be at least 1 minute")
    val durationMinutes: Int,
    
    val classId: Long? = null,
    
    @field:Min(value = 0, message = "Calories burned cannot be negative")
    val caloriesBurned: Int? = null,
    
    @field:Size(max = 500, message = "Notes cannot exceed 500 characters")
    val notes: String? = null
)