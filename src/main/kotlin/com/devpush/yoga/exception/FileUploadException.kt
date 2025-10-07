package com.devpush.yoga.exception

/**
 * Exception thrown when file upload operations fail
 * Used for various file handling errors including validation, storage, and security issues
 */
class FileUploadException(
    message: String,
    val errorCode: String = "FILE_UPLOAD_ERROR",
    cause: Throwable? = null
) : RuntimeException(message, cause) {
    
    companion object {
        // Error codes for different file upload scenarios
        const val INVALID_FILE_TYPE = "INVALID_FILE_TYPE"
        const val FILE_TOO_LARGE = "FILE_TOO_LARGE"
        const val FILE_EMPTY = "FILE_EMPTY"
        const val STORAGE_ERROR = "STORAGE_ERROR"
        const val SECURITY_VIOLATION = "SECURITY_VIOLATION"
        const val INVALID_FILE_NAME = "INVALID_FILE_NAME"
        const val CLEANUP_ERROR = "CLEANUP_ERROR"
    }
}