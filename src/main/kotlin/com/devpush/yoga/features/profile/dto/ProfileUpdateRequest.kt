package com.devpush.yoga.features.profile.dto

import com.devpush.yoga.entity.FitnessLevel
import jakarta.validation.constraints.Size

data class ProfileUpdateRequest(
    @field:Size(max = 100, message = "Name cannot exceed 100 characters")
    val name: String? = null,
    
    @field:Size(max = 500, message = "Bio cannot exceed 500 characters")
    val bio: String? = null,
    
    val fitnessLevel: FitnessLevel? = null,
    
    @field:Size(max = 1000, message = "Preferences cannot exceed 1000 characters")
    val preferences: String? = null
)