package com.devpush.yoga.dto

import com.devpush.yoga.entity.OAuthProvider

data class UserProfile(
    val id: Long,
    val email: String,
    val name: String?,
    val profilePicture: String?,
    val provider: OAuthProvider
)