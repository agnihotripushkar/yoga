package com.devpush.yoga.exception

/**
 * Exception thrown when rate limiting is exceeded
 */
class RateLimitExceededException(
    message: String = "Rate limit exceeded. Too many requests.",
    cause: Throwable? = null
) : RuntimeException(message, cause)