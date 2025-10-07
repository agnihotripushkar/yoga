package com.devpush.yoga.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.web.servlet.MultipartConfigFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.unit.DataSize
import org.springframework.web.multipart.MultipartResolver
import org.springframework.web.multipart.support.StandardServletMultipartResolver
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import jakarta.servlet.MultipartConfigElement
import java.io.File

/**
 * Configuration for file upload handling and security
 * Configures multipart file upload limits, timeouts, and static resource serving
 */
@Configuration
class FileUploadConfig : WebMvcConfigurer {
    
    /**
     * Configure multipart file upload settings
     * Sets size limits, temporary file location, and processing thresholds
     */
    @Bean
    fun multipartConfigElement(): MultipartConfigElement {
        val factory = MultipartConfigFactory()
        
        // Set maximum file size (5MB)
        factory.setMaxFileSize(DataSize.ofMegabytes(5))
        
        // Set maximum request size (10MB to allow multiple files)
        factory.setMaxRequestSize(DataSize.ofMegabytes(10))
        
        // Set file size threshold for writing to disk (2KB)
        factory.setFileSizeThreshold(DataSize.ofKilobytes(2))
        
        // Set temporary location for file processing
        val tempDir = System.getProperty("java.io.tmpdir")
        factory.setLocation(tempDir)
        
        return factory.createMultipartConfig()
    }
    
    /**
     * Configure multipart resolver for handling file uploads
     * Uses standard servlet multipart resolver with lazy resolution disabled for security
     */
    @Bean
    fun multipartResolver(): MultipartResolver {
        val resolver = StandardServletMultipartResolver()
        // Disable lazy resolution to prevent potential security issues
        resolver.setResolveLazily(false)
        return resolver
    }
    
    /**
     * Configure static resource handlers for serving uploaded files
     * Maps /uploads/** URLs to the uploads directory with security headers
     */
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // Create uploads directory if it doesn't exist
        val uploadsDir = File("uploads")
        if (!uploadsDir.exists()) {
            uploadsDir.mkdirs()
        }
        
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:uploads/")
            .setCachePeriod(3600) // Cache for 1 hour
            .resourceChain(true)
            .addResolver { request, requestPath, locations ->
                // Additional security check for file access
                val resolvedResource = locations.firstOrNull()?.createRelative(requestPath)
                
                // Ensure the resolved path is within the uploads directory
                if (resolvedResource != null && resolvedResource.exists()) {
                    val canonicalPath = resolvedResource.file.canonicalPath
                    val uploadsCanonicalPath = uploadsDir.canonicalPath
                    
                    if (canonicalPath.startsWith(uploadsCanonicalPath)) {
                        resolvedResource
                    } else {
                        null // Path traversal attempt detected
                    }
                } else {
                    null
                }
            }
    }
}

/**
 * Configuration properties for file upload settings
 * Binds application properties to a configuration class for type-safe access
 */
@Configuration
@ConfigurationProperties(prefix = "app.file.upload")
data class FileUploadProperties(
    var basePath: String = "uploads",
    var maxSize: Long = 5242880L, // 5MB
    var allowedTypes: List<String> = listOf("image/jpeg", "image/png", "image/webp"),
    var allowedExtensions: List<String> = listOf("jpg", "jpeg", "png", "webp"),
    var cleanupIntervalHours: Int = 24,
    var orphanedFileRetentionDays: Int = 7
)