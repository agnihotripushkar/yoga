package com.devpush.yoga.exception

/**
 * Exception thrown when progress tracking operations fail
 * Used for session recording, analytics calculation, and progress data errors
 */
class ProgressException(
    message: String,
    val errorCode: String = "PROGRESS_ERROR",
    cause: Throwable? = null
) : RuntimeException(message, cause) {
    
    companion object {
        // Error codes for different progress scenarios
        const val SESSION_RECORDING_FAILED = "SESSION_RECORDING_FAILED"
        const val INVALID_SESSION_DATA = "INVALID_SESSION_DATA"
        const val INVALID_DURATION = "INVALID_DURATION"
        const val INVALID_CALORIES = "INVALID_CALORIES"
        const val ANALYTICS_CALCULATION_FAILED = "ANALYTICS_CALCULATION_FAILED"
        const val PROGRESS_DATA_NOT_FOUND = "PROGRESS_DATA_NOT_FOUND"
        const val DATE_RANGE_INVALID = "DATE_RANGE_INVALID"
        const val SESSION_NOT_FOUND = "SESSION_NOT_FOUND"
        const val DUPLICATE_SESSION = "DUPLICATE_SESSION"
        const val PROGRESS_ACCESS_DENIED = "PROGRESS_ACCESS_DENIED"
        const val CALORIE_ESTIMATION_FAILED = "CALORIE_ESTIMATION_FAILED"
    }
}