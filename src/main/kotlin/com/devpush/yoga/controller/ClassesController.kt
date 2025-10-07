package com.devpush.yoga.controller

import com.devpush.yoga.dto.*
import com.devpush.yoga.service.ClassesService
import com.devpush.yoga.service.JwtTokenManager
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/classes")
class ClassesController(
    private val classesService: ClassesService,
    private val jwtTokenManager: JwtTokenManager
) {
    
    private val logger = LoggerFactory.getLogger(ClassesController::class.java)
    
    @GetMapping
    fun getClasses(
        @RequestHeader("Authorization") authorization: String,
        @Valid @ModelAttribute searchRequest: ClassSearchRequest
    ): ResponseEntity<ClassListResponse> {
        logger.debug("Received classes list request with filters")
        
        return try {
            val userId = extractAndValidateUserId(authorization)
            val classListResponse = classesService.getClasses(searchRequest, userId)
            ResponseEntity.ok(classListResponse)
        } catch (ex: Exception) {
            logger.error("Classes retrieval failed: {}", ex.message)
            throw RuntimeException("Classes retrieval failed", ex)
        }
    }
    
    @GetMapping("/{classId}")
    fun getClassById(
        @RequestHeader("Authorization") authorization: String,
        @PathVariable classId: Long
    ): ResponseEntity<YogaClassResponse> {
        logger.debug("Received class details request for classId: {}", classId)
        
        return try {
            val userId = extractAndValidateUserId(authorization)
            val classResponse = classesService.getClassById(classId, userId)
            ResponseEntity.ok(classResponse)
        } catch (ex: Exception) {
            logger.error("Class details retrieval failed for classId: {}: {}", classId, ex.message)
            throw RuntimeException("Class details retrieval failed", ex)
        }
    }
    
    @GetMapping("/search")
    fun searchClasses(
        @RequestHeader("Authorization") authorization: String,
        @Valid @ModelAttribute searchRequest: ClassSearchRequest
    ): ResponseEntity<ClassListResponse> {
        logger.debug("Received class search request with query: '{}'", searchRequest.query)
        
        return try {
            val userId = extractAndValidateUserId(authorization)
            val searchResults = classesService.searchClasses(searchRequest, userId)
            ResponseEntity.ok(searchResults)
        } catch (ex: Exception) {
            logger.error("Class search failed for query '{}': {}", searchRequest.query, ex.message)
            throw RuntimeException("Class search failed", ex)
        }
    }
    
    @PostMapping("/{classId}/favorite")
    fun addToFavorites(
        @RequestHeader("Authorization") authorization: String,
        @PathVariable classId: Long
    ): ResponseEntity<FavoriteResponse> {
        logger.info("Received add to favorites request for classId: {}", classId)
        
        return try {
            val userId = extractAndValidateUserId(authorization)
            val favoriteResponse = classesService.addToFavorites(classId, userId)
            val status = if (favoriteResponse.success) HttpStatus.CREATED else HttpStatus.CONFLICT
            ResponseEntity.status(status).body(favoriteResponse)
        } catch (ex: Exception) {
            logger.error("Add to favorites failed for classId: {}: {}", classId, ex.message)
            throw RuntimeException("Add to favorites failed", ex)
        }
    }
    
    @DeleteMapping("/{classId}/favorite")
    fun removeFromFavorites(
        @RequestHeader("Authorization") authorization: String,
        @PathVariable classId: Long
    ): ResponseEntity<FavoriteResponse> {
        logger.info("Received remove from favorites request for classId: {}", classId)
        
        return try {
            val userId = extractAndValidateUserId(authorization)
            val favoriteResponse = classesService.removeFromFavorites(classId, userId)
            val status = if (favoriteResponse.success) HttpStatus.OK else HttpStatus.NOT_FOUND
            ResponseEntity.status(status).body(favoriteResponse)
        } catch (ex: Exception) {
            logger.error("Remove from favorites failed for classId: {}: {}", classId, ex.message)
            throw RuntimeException("Remove from favorites failed", ex)
        }
    }
    
    @GetMapping("/favorites")
    fun getFavoriteClasses(
        @RequestHeader("Authorization") authorization: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<List<FavoriteClassResponse>> {
        logger.debug("Received favorite classes request for page: {}, size: {}", page, size)
        
        return try {
            val userId = extractAndValidateUserId(authorization)
            
            if (page < 0) {
                throw IllegalArgumentException("Page number cannot be negative")
            }
            if (size < 1 || size > 100) {
                throw IllegalArgumentException("Page size must be between 1 and 100")
            }
            
            val favoriteClasses = classesService.getFavoriteClasses(userId, page, size)
            ResponseEntity.ok(favoriteClasses)
        } catch (ex: Exception) {
            logger.error("Favorite classes retrieval failed: {}", ex.message)
            throw RuntimeException("Favorite classes retrieval failed", ex)
        }
    }
    
    private fun extractAndValidateUserId(authorization: String): Long {
        val token = extractTokenFromHeader(authorization)
            ?: throw RuntimeException("Invalid authorization header format")
        
        val userId = jwtTokenManager.getUserIdFromToken(token)
            ?: throw RuntimeException("Invalid or expired access token")
        
        if (!jwtTokenManager.isAccessToken(token)) {
            throw RuntimeException("Invalid token type. Access token required")
        }
        
        return userId
    }
    
    private fun extractTokenFromHeader(authorization: String): String? {
        return if (authorization.startsWith("Bearer ", ignoreCase = true)) {
            authorization.substring(7)
        } else {
            null
        }
    }
}