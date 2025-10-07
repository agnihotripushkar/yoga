package com.devpush.yoga.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import org.springframework.web.multipart.MultipartFile
import kotlin.reflect.KClass

/**
 * Custom validation annotation for validating file sizes
 * Ensures uploaded files don't exceed specified size limits
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [FileSizeValidator::class])
@MustBeDocumented
annotation class ValidFileSize(
    val message: String = "File size exceeds maximum allowed size",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
    val maxSizeBytes: Long = 5242880L, // 5MB default
    val maxSizeMB: Int = 5 // For display in error messages
)

/**
 * Validator implementation for file size validation
 */
class FileSizeValidator : ConstraintValidator<ValidFileSize, MultipartFile?> {
    
    private var maxSizeBytes: Long = 0
    private var maxSizeMB: Int = 0
    
    override fun initialize(constraintAnnotation: ValidFileSize) {
        maxSizeBytes = constraintAnnotation.maxSizeBytes
        maxSizeMB = constraintAnnotation.maxSizeMB
    }
    
    override fun isValid(file: MultipartFile?, context: ConstraintValidatorContext): Boolean {
        // Null files are considered valid (use @NotNull for null checks)
        if (file == null || file.isEmpty) {
            return true
        }
        
        if (file.size > maxSizeBytes) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate(
                "File size (${formatFileSize(file.size)}) exceeds maximum allowed size of ${maxSizeMB}MB"
            ).addConstraintViolation()
            return false
        }
        
        return true
    }
    
    private fun formatFileSize(sizeBytes: Long): String {
        return when {
            sizeBytes < 1024 -> "${sizeBytes}B"
            sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024}KB"
            else -> "${sizeBytes / (1024 * 1024)}MB"
        }
    }
}