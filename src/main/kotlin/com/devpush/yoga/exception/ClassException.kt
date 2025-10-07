package com.devpush.yoga.exception

/**
 * Exception thrown when yoga class operations fail
 * Used for class retrieval, search, favorites management, and video URL validation errors
 */
class ClassException(
    message: String,
    val errorCode: String = "CLASS_ERROR",
    cause: Throwable? = null
) : RuntimeException(message, cause) {
    
    companion object {
        // Error codes for different class scenarios
        const val CLASS_NOT_FOUND = "CLASS_NOT_FOUND"
        const val INVALID_CLASS_DATA = "INVALID_CLASS_DATA"
        const val VIDEO_URL_INVALID = "VIDEO_URL_INVALID"
        const val VIDEO_URL_EXPIRED = "VIDEO_URL_EXPIRED"
        const val VIDEO_NOT_ACCESSIBLE = "VIDEO_NOT_ACCESSIBLE"
        const val CLASS_SEARCH_FAILED = "CLASS_SEARCH_FAILED"
        const val INVALID_SEARCH_CRITERIA = "INVALID_SEARCH_CRITERIA"
        const val FAVORITE_ALREADY_EXISTS = "FAVORITE_ALREADY_EXISTS"
        const val FAVORITE_NOT_FOUND = "FAVORITE_NOT_FOUND"
        const val FAVORITE_OPERATION_FAILED = "FAVORITE_OPERATION_FAILED"
        const val CLASS_ACCESS_DENIED = "CLASS_ACCESS_DENIED"
        const val INVALID_DIFFICULTY_LEVEL = "INVALID_DIFFICULTY_LEVEL"
        const val INVALID_DURATION_FILTER = "INVALID_DURATION_FILTER"
        const val PAGINATION_ERROR = "PAGINATION_ERROR"
    }
}