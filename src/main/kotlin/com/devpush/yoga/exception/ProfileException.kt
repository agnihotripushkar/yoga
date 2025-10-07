package com.devpush.yoga.exception

/**
 * Exception thrown when profile-related operations fail
 * Used for profile validation, update, and management errors
 */
class ProfileException(
    message: String,
    val errorCode: String = "PROFILE_ERROR",
    cause: Throwable? = null
) : RuntimeException(message, cause) {
    
    companion object {
        // Error codes for different profile scenarios
        const val PROFILE_NOT_FOUND = "PROFILE_NOT_FOUND"
        const val INVALID_PROFILE_DATA = "INVALID_PROFILE_DATA"
        const val PROFILE_UPDATE_FAILED = "PROFILE_UPDATE_FAILED"
        const val BIO_TOO_LONG = "BIO_TOO_LONG"
        const val INVALID_FITNESS_LEVEL = "INVALID_FITNESS_LEVEL"
        const val PROFILE_PICTURE_ERROR = "PROFILE_PICTURE_ERROR"
        const val PREFERENCES_INVALID = "PREFERENCES_INVALID"
        const val PROFILE_ACCESS_DENIED = "PROFILE_ACCESS_DENIED"
        const val PROFILE_VALIDATION_FAILED = "PROFILE_VALIDATION_FAILED"
    }
}