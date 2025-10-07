package com.devpush.yoga.features.auth.dto

import com.devpush.yoga.entity.OAuthProvider

data class OAuthUserInfo(
    val providerId: String,
    val email: String,
    val name: String?,
    val profilePicture: String?,
    val provider: OAuthProvider
)