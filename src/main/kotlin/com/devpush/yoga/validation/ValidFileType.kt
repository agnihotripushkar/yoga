package com.devpush.yoga.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import org.springframework.web.multipart.MultipartFile
import kotlin.reflect.KClass

/**
 * Custom validation annotation for validating file types
 * Validates both MIME type and file extension for security
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [FileTypeValidator::class])
@MustBeDocumented
annotation class ValidFileType(
    val message: String = "Invalid file type",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
    val allowedTypes: Array<String> = ["image/jpeg", "image/png", "image/webp"],
    val allowedExtensions: Array<String> = ["jpg", "jpeg", "png", "webp"]
)

/**
 * Validator implementation for file type validation
 */
class FileTypeValidator : ConstraintValidator<ValidFileType, MultipartFile?> {
    
    private lateinit var allowedTypes: Set<String>
    private lateinit var allowedExtensions: Set<String>
    
    override fun initialize(constraintAnnotation: ValidFileType) {
        allowedTypes = constraintAnnotation.allowedTypes.toSet()
        allowedExtensions = constraintAnnotation.allowedExtensions.map { it.lowercase() }.toSet()
    }
    
    override fun isValid(file: MultipartFile?, context: ConstraintValidatorContext): Boolean {
        // Null files are considered valid (use @NotNull for null checks)
        if (file == null || file.isEmpty) {
            return true
        }
        
        // Validate MIME type
        val contentType = file.contentType
        if (contentType == null || !allowedTypes.contains(contentType)) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate(
                "File type not allowed. Allowed types: ${allowedTypes.joinToString(", ")}"
            ).addConstraintViolation()
            return false
        }
        
        // Validate file extension
        val originalFilename = file.originalFilename ?: ""
        val extension = if (originalFilename.contains('.')) {
            originalFilename.substringAfterLast('.').lowercase()
        } else {
            ""
        }
        
        if (extension.isEmpty() || !allowedExtensions.contains(extension)) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate(
                "File extension not allowed. Allowed extensions: ${allowedExtensions.joinToString(", ")}"
            ).addConstraintViolation()
            return false
        }
        
        return true
    }
}