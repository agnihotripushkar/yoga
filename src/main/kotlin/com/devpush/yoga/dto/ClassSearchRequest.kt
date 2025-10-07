package com.devpush.yoga.dto

import com.devpush.yoga.entity.DifficultyLevel
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

data class ClassSearchRequest(
    @field:Size(max = 100, message = "Search query cannot exceed 100 characters")
    val query: String? = null,
    
    val difficultyLevel: DifficultyLevel? = null,
    
    @field:Min(value = 1, message = "Minimum duration must be at least 1 minute")
    @field:Max(value = 300, message = "Maximum duration cannot exceed 300 minutes")
    val minDuration: Int? = null,
    
    @field:Min(value = 1, message = "Maximum duration must be at least 1 minute")
    @field:Max(value = 300, message = "Maximum duration cannot exceed 300 minutes")
    val maxDuration: Int? = null,
    
    @field:Size(max = 100, message = "Instructor name cannot exceed 100 characters")
    val instructor: String? = null,
    
    @field:Min(value = 0, message = "Page number cannot be negative")
    val page: Int = 0,
    
    @field:Min(value = 1, message = "Page size must be at least 1")
    @field:Max(value = 100, message = "Page size cannot exceed 100")
    val size: Int = 20,
    
    @field:Size(max = 50, message = "Sort field cannot exceed 50 characters")
    val sortBy: String = "title",
    
    val sortDirection: String = "asc"
)