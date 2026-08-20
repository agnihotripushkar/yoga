package com.devpush.yoga.features.classes.controller

import com.devpush.yoga.features.classes.dto.*
import com.devpush.yoga.features.classes.service.ClassesService
import com.devpush.yoga.features.auth.service.JwtTokenManager
import com.devpush.yoga.service.RateLimitService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/classes")
@Tag(name = "Yoga Classes", description = "Yoga class catalog, search, and favorites management")
class ClassesController(
    private val classesService: ClassesService,
    private val jwtTokenManager: JwtTokenManager,
    private val rateLimitService: RateLimitService
) {
    
    private val logger = LoggerFactory.getLogger(ClassesController::class.java)
    
    @Operation(
        summary = "Get Yoga Classes",
        description = "Retrieve paginated list of yoga classes with optional filtering by difficulty, duration, and instructor",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Classes retrieved successfully",
                content = [Content(schema = Schema(implementation = ClassListResponse::class))]
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "429", description = "Rate limit exceeded")
        ]
    )
    @GetMapping
    fun getClasses(
        @Parameter(description = "JWT access token")
        @RequestHeader("Authorization") authorization: String,
        @Parameter(description = "Search and filter parameters")
        @Valid @ModelAttribute searchRequest: ClassSearchRequest
    ): ResponseEntity<ClassListResponse> {
        logger.debug("Received classes list request with filters")
        
        return try {
            val userId = extractAndValidateUserId(authorization)
            
            // Apply rate limiting for class searches
            rateLimitService.checkClassSearchRateLimit(userId.toString())
            
            val classListResponse = classesService.getClasses(searchRequest, userId)
            ResponseEntity.ok(classListResponse)
        } catch (ex: Exception) {
            logger.error("Classes retrieval failed: {}", ex.message)
            throw RuntimeException("Classes retrieval failed", ex)
        }
    }
    
    @Operation(
        summary = "Get Class Details",
        description = "Retrieve detailed information about a specific yoga class including video URL",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Class details retrieved successfully",
                content = [Content(schema = Schema(implementation = YogaClassResponse::class))]
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "404", description = "Class not found")
        ]
    )
    @GetMapping("/{classId}")
    fun getClassById(
        @Parameter(description = "JWT access token")
        @RequestHeader("Authorization") authorization: String,
        @Parameter(description = "Yoga class ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable classId: UUID
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
    
    @Operation(
        summary = "Search Yoga Classes",
        description = "Search yoga classes by title, description, or instructor name with optional filters",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Search results retrieved successfully",
                content = [Content(schema = Schema(implementation = ClassListResponse::class))]
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "429", description = "Rate limit exceeded")
        ]
    )
    @GetMapping("/search")
    fun searchClasses(
        @Parameter(description = "JWT access token")
        @RequestHeader("Authorization") authorization: String,
        @Parameter(description = "Search parameters including query text and filters")
        @Valid @ModelAttribute searchRequest: ClassSearchRequest
    ): ResponseEntity<ClassListResponse> {
        logger.debug("Received class search request with query: '{}'", searchRequest.query)
        
        return try {
            val userId = extractAndValidateUserId(authorization)
            
            // Apply rate limiting for class searches
            rateLimitService.checkClassSearchRateLimit(userId.toString())
            
            val searchResults = classesService.searchClasses(searchRequest, userId)
            ResponseEntity.ok(searchResults)
        } catch (ex: Exception) {
            logger.error("Class search failed for query '{}': {}", searchRequest.query, ex.message)
            throw RuntimeException("Class search failed", ex)
        }
    }
    
    @Operation(
        summary = "Add Class to Favorites",
        description = "Bookmark a yoga class to the user's favorites list",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Class added to favorites successfully",
                content = [Content(schema = Schema(implementation = FavoriteResponse::class))]
            ),
            ApiResponse(responseCode = "409", description = "Class already in favorites"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "404", description = "Class not found")
        ]
    )
    @PostMapping("/{classId}/favorite")
    fun addToFavorites(
        @Parameter(description = "JWT access token")
        @RequestHeader("Authorization") authorization: String,
        @Parameter(description = "Yoga class ID to add to favorites", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable classId: UUID
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
    
    @Operation(
        summary = "Remove Class from Favorites",
        description = "Remove a yoga class from the user's favorites list",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Class removed from favorites successfully",
                content = [Content(schema = Schema(implementation = FavoriteResponse::class))]
            ),
            ApiResponse(responseCode = "404", description = "Class not in favorites or not found"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    @DeleteMapping("/{classId}/favorite")
    fun removeFromFavorites(
        @Parameter(description = "JWT access token")
        @RequestHeader("Authorization") authorization: String,
        @Parameter(description = "Yoga class ID to remove from favorites", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable classId: UUID
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
    
    @Operation(
        summary = "Get Favorite Classes",
        description = "Retrieve the user's bookmarked yoga classes with pagination",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Favorite classes retrieved successfully",
                content = [Content(schema = Schema(implementation = Array<FavoriteClassResponse>::class))]
            ),
            ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    @GetMapping("/favorites")
    fun getFavoriteClasses(
        @Parameter(description = "JWT access token")
        @RequestHeader("Authorization") authorization: String,
        @Parameter(description = "Page number (0-based)", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size (1-100)", example = "20")
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
    
    private fun extractAndValidateUserId(authorization: String): UUID {
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