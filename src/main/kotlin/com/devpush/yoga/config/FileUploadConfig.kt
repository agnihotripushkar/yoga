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

@Configuration
class FileUploadConfig : WebMvcConfigurer {
    
    @Bean
    fun multipartConfigElement(): MultipartConfigElement {
        val factory = MultipartConfigFactory()
        factory.setMaxFileSize(DataSize.ofMegabytes(5))
        factory.setMaxRequestSize(DataSize.ofMegabytes(10))
        factory.setFileSizeThreshold(DataSize.ofKilobytes(2))
        val tempDir = System.getProperty("java.io.tmpdir")
        factory.setLocation(tempDir)
        return factory.createMultipartConfig()
    }
    
    @Bean
    fun multipartResolver(): MultipartResolver {
        val resolver = StandardServletMultipartResolver()
        resolver.setResolveLazily(false)
        return resolver
    }
    
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val uploadsDir = File("uploads")
        if (!uploadsDir.exists()) {
            uploadsDir.mkdirs()
        }
        
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:uploads/")
            .setCachePeriod(3600)
    }
}

@Configuration
@ConfigurationProperties(prefix = "app.file.upload")
data class FileUploadProperties(
    var basePath: String = "uploads",
    var maxSize: Long = 5242880L,
    var allowedTypes: List<String> = listOf("image/jpeg", "image/png", "image/webp"),
    var allowedExtensions: List<String> = listOf("jpg", "jpeg", "png", "webp"),
    var cleanupIntervalHours: Int = 24,
    var orphanedFileRetentionDays: Int = 7
)