package com.devpush.yoga.features.auth.dto

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long, // in seconds
    val user: UserProfile
)