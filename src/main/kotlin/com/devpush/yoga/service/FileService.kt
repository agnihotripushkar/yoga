package com.devpush.yoga.service

import com.devpush.yoga.exception.FileUploadException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Service for secure file upload, storage, and management operations
 * Provides comprehensive file validation, security scanning, and cleanup functionality
 */
@Service
class FileService {
    
    private val logger = LoggerFactory.getLogger(FileService::class.java)
    
    @Value("\${app.file.upload.base-path:uploads}")
    private lateinit var baseUploadPath: String
    
    @Value("\${app.file.upload.max-size:5242880}") // 5MB default
    private var maxFileSize: Long = 5242880
    
    @Value("\${app.file.upload.allowed-types:image/jpeg,image/png,image/webp}")
    private lateinit var allowedMimeTypes: String
    
    @Value("\${app.file.upload.allowed-extensions:jpg,jpeg,png,webp}")
    private lateinit var allowedExtensions: String
    
    // Magic number signatures for file type validation
    private val magicNumbers = mapOf(
        "image/jpeg" to listOf(
            byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()),
            byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()),
            byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE1.toByte())
        ),
        "image/png" to listOf(
            byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 
                       0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte())
        ),
        "image/webp" to listOf(
            byteArrayOf(0x52.toByte(), 0x49.toByte(), 0x46.toByte(), 0x46.toByte()) // RIFF header
        )
    )
    
    /**
     * Store a file securely with comprehensive validation
     */
    fun storeFile(file: MultipartFile, category: String, userId: Long? = null): String {
        logger.info("Storing file: {} in category: {} for user: {}", 
                   file.originalFilename, category, userId)
        
        validateFile(file)
        val fileName = generateSecureFileName(file, userId)
        val storagePath = createStoragePath(category, userId)
        
        try {
            Files.createDirectories(storagePath)
            val filePath = storagePath.resolve(fileName)
            Files.copy(file.inputStream, filePath, StandardCopyOption.REPLACE_EXISTING)
            
            if (!Files.exists(filePath) || Files.size(filePath) != file.size) {
                throw FileUploadException(
                    "File storage verification failed",
                    FileUploadException.STORAGE_ERROR
                )
            }
            
            val relativePath = getRelativePath(filePath)
            logger.info("Successfully stored file: {} at path: {}", fileName, relativePath)
            return relativePath
            
        } catch (ex: IOException) {
            logger.error("Failed to store file: {}", fileName, ex)
            throw FileUploadException(
                "Failed to store file: ${ex.message}",
                FileUploadException.STORAGE_ERROR,
                ex
            )
        }
    }
    
    /**
     * Retrieve file information and validate access
     */
    fun getFile(filePath: String): File {
        logger.debug("Retrieving file: {}", filePath)
        
        val fullPath = Paths.get(filePath)
        val normalizedPath = fullPath.normalize()
        val basePath = Paths.get(baseUploadPath).normalize()
        
        if (!normalizedPath.startsWith(basePath)) {
            logger.warn("Attempted path traversal attack: {}", filePath)
            throw FileUploadException(
                "Invalid file path",
                FileUploadException.SECURITY_VIOLATION
            )
        }
        
        val file = normalizedPath.toFile()
        
        if (!file.exists()) {
            throw FileUploadException(
                "File not found: $filePath",
                "FILE_NOT_FOUND"
            )
        }
        
        if (!file.canRead()) {
            throw FileUploadException(
                "File not accessible: $filePath",
                "FILE_ACCESS_DENIED"
            )
        }
        
        return file
    }
    
    /**
     * Delete a file securely
     */
    fun deleteFile(filePath: String): Boolean {
        logger.info("Deleting file: {}", filePath)
        
        try {
            val fullPath = Paths.get(filePath)
            val normalizedPath = fullPath.normalize()
            val basePath = Paths.get(baseUploadPath).normalize()
            
            if (!normalizedPath.startsWith(basePath)) {
                logger.warn("Attempted path traversal attack during deletion: {}", filePath)
                throw FileUploadException(
                    "Invalid file path for deletion",
                    FileUploadException.SECURITY_VIOLATION
                )
            }
            
            if (Files.exists(normalizedPath)) {
                Files.delete(normalizedPath)
                logger.info("Successfully deleted file: {}", filePath)
                return true
            } else {
                logger.debug("File not found for deletion: {}", filePath)
                return false
            }
            
        } catch (ex: IOException) {
            logger.error("Failed to delete file: {}", filePath, ex)
            throw FileUploadException(
                "Failed to delete file: ${ex.message}",
                FileUploadException.CLEANUP_ERROR,
                ex
            )
        }
    }
    
    /**
     * Clean up orphaned files in a directory
     */
    fun cleanupOrphanedFiles(category: String, olderThanDays: Int = 7): Int {
        logger.info("Cleaning up orphaned files in category: {} older than {} days", 
                   category, olderThanDays)
        
        val categoryPath = Paths.get(baseUploadPath, category)
        
        if (!Files.exists(categoryPath)) {
            logger.debug("Category path does not exist: {}", categoryPath)
            return 0
        }
        
        var cleanedCount = 0
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        
        try {
            Files.walk(categoryPath)
                .filter { Files.isRegularFile(it) }
                .filter { Files.getLastModifiedTime(it).toMillis() < cutoffTime }
                .forEach { filePath ->
                    try {
                        Files.delete(filePath)
                        cleanedCount++
                        logger.debug("Cleaned up orphaned file: {}", filePath)
                    } catch (ex: IOException) {
                        logger.warn("Failed to delete orphaned file: {}", filePath, ex)
                    }
                }
        } catch (ex: IOException) {
            logger.error("Error during cleanup of category: {}", category, ex)
            throw FileUploadException(
                "Cleanup operation failed: ${ex.message}",
                FileUploadException.CLEANUP_ERROR,
                ex
            )
        }
        
        logger.info("Cleaned up {} orphaned files in category: {}", cleanedCount, category)
        return cleanedCount
    }
    
    private fun validateFile(file: MultipartFile) {
        if (file.isEmpty) {
            throw FileUploadException(
                "File cannot be empty",
                FileUploadException.FILE_EMPTY
            )
        }
        
        if (file.size > maxFileSize) {
            throw FileUploadException(
                "File size exceeds maximum allowed size of ${maxFileSize / 1024 / 1024}MB",
                FileUploadException.FILE_TOO_LARGE
            )
        }
        
        val contentType = file.contentType
        if (contentType == null || !getAllowedMimeTypes().contains(contentType)) {
            throw FileUploadException(
                "File type not allowed. Allowed types: ${getAllowedMimeTypes()}",
                FileUploadException.INVALID_FILE_TYPE
            )
        }
        
        val originalFilename = file.originalFilename ?: ""
        val extension = extractFileExtension(originalFilename)
        if (!getAllowedExtensions().contains(extension.lowercase())) {
            throw FileUploadException(
                "File extension not allowed. Allowed extensions: ${getAllowedExtensions()}",
                FileUploadException.INVALID_FILE_TYPE
            )
        }
        
        validateFileContent(file, contentType)
        validateFileName(originalFilename)
    }
    
    private fun validateFileContent(file: MultipartFile, expectedMimeType: String) {
        try {
            val fileBytes = file.bytes
            if (fileBytes.size < 8) {
                throw FileUploadException(
                    "File too small to validate content",
                    FileUploadException.SECURITY_VIOLATION
                )
            }
            
            val magicNumbersForType = magicNumbers[expectedMimeType]
            if (magicNumbersForType != null) {
                val isValid = magicNumbersForType.any { signature ->
                    fileBytes.take(signature.size).toByteArray().contentEquals(signature)
                }
                
                if (!isValid) {
                    logger.warn("File content does not match declared MIME type: {}", expectedMimeType)
                    throw FileUploadException(
                        "File content does not match declared file type",
                        FileUploadException.SECURITY_VIOLATION
                    )
                }
            }
        } catch (ex: IOException) {
            logger.error("Failed to read file content for validation", ex)
            throw FileUploadException(
                "Failed to validate file content",
                FileUploadException.SECURITY_VIOLATION,
                ex
            )
        }
    }
    
    private fun validateFileName(filename: String) {
        if (filename.isBlank()) {
            throw FileUploadException(
                "Filename cannot be empty",
                FileUploadException.INVALID_FILE_NAME
            )
        }
        
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw FileUploadException(
                "Filename contains invalid characters",
                FileUploadException.SECURITY_VIOLATION
            )
        }
        
        val dangerousChars = charArrayOf('\u0000', '<', '>', ':', '"', '|', '?', '*')
        if (filename.any { it in dangerousChars }) {
            throw FileUploadException(
                "Filename contains dangerous characters",
                FileUploadException.SECURITY_VIOLATION
            )
        }
        
        if (filename.length > 255) {
            throw FileUploadException(
                "Filename too long (max 255 characters)",
                FileUploadException.INVALID_FILE_NAME
            )
        }
    }
    
    private fun generateSecureFileName(file: MultipartFile, userId: Long?): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val originalFilename = file.originalFilename ?: "file"
        val extension = extractFileExtension(originalFilename)
        
        val contentHash = try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(file.bytes)
            hashBytes.joinToString("") { "%02x".format(it) }.take(8)
        } catch (ex: Exception) {
            logger.warn("Failed to generate content hash, using random string", ex)
            UUID.randomUUID().toString().take(8)
        }
        
        val userPrefix = userId?.let { "user_${it}_" } ?: ""
        return "${userPrefix}${timestamp}_${contentHash}.${extension}"
    }
    
    private fun createStoragePath(category: String, userId: Long?): Path {
        val pathComponents = mutableListOf(baseUploadPath, category)
        userId?.let { pathComponents.add("user_$it") }
        return Paths.get(pathComponents.first(), *pathComponents.drop(1).toTypedArray())
    }
    
    private fun getRelativePath(absolutePath: Path): String {
        val basePath = Paths.get(baseUploadPath).toAbsolutePath().normalize()
        val filePath = absolutePath.toAbsolutePath().normalize()
        return basePath.relativize(filePath).toString().replace("\\", "/")
    }
    
    private fun extractFileExtension(filename: String): String {
        return if (filename.contains('.')) {
            filename.substringAfterLast('.')
        } else {
            ""
        }
    }
    
    private fun getAllowedMimeTypes(): Set<String> {
        return allowedMimeTypes.split(",").map { it.trim() }.toSet()
    }
    
    private fun getAllowedExtensions(): Set<String> {
        return allowedExtensions.split(",").map { it.trim().lowercase() }.toSet()
    }
}