package com.devpush.yoga.features.auth.dto

import jakarta.validation.constraints.NotBlank

data class GoogleLoginRequest(
    @field:NotBlank(message = "ID token is required")
    val idToken: String
)