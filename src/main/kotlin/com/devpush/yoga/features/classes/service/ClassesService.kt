package com.devpush.yoga.features.classes.service

import com.devpush.yoga.features.classes.dto.*
import com.devpush.yoga.entity.ClassFavorite
import com.devpush.yoga.entity.User
import com.devpush.yoga.entity.YogaClass
import com.devpush.yoga.repository.ClassFavoriteRepository
import com.devpush.yoga.repository.UserRepository
import com.devpush.yoga.repository.YogaClassRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URL
import java.time.LocalDateTime

@Service
@Transactional
class ClassesService(
    private val yogaClassRepository: YogaClassRepository,
    private val classFavoriteRepository: ClassFavoriteRepository,
    private val userRepository: UserRepository
) {
    
    private val logger = LoggerFactory.getLogger(ClassesService::class.java)
    
    /**
     * Get all classes with pagination and filtering
     */
    fun getClasses(searchRequest: ClassSearchRequest, userId: Long): ClassListResponse {
        logger.debug("Getting classes with filters for userId: {}", userId)
        
        val user = getUserById(userId)
        val pageable = createPageable(searchRequest)
        
        val classesPage = if (hasFilters(searchRequest)) {
            yogaClassRepository.findByFilters(
                difficultyLevel = searchRequest.difficultyLevel,
                minDuration = searchRequest.minDuration,
                maxDuration = searchRequest.maxDuration,
                instructor = searchRequest.instructor,
                pageable = pageable
            )
        } else {
            yogaClassRepository.findAll(pageable)
        }
        
        val userFavorites = classFavoriteRepository.findFavoriteClassesByUser(user).map { it.id }.toSet()
        
        val classSummaries = classesPage.content.map { yogaClass ->
            toYogaClassSummary(yogaClass, userFavorites.contains(yogaClass.id))
        }
        
        val pagination = PaginationMetadata(
            currentPage = classesPage.number,
            totalPages = classesPage.totalPages,
            totalElements = classesPage.totalElements,
            pageSize = classesPage.size,
            hasNext = classesPage.hasNext(),
            hasPrevious = classesPage.hasPrevious()
        )
        
        return ClassListResponse(
            classes = classSummaries,
            pagination = pagination
        )
    }
    
    /**
     * Get a specific class by ID
     */
    fun getClassById(classId: Long, userId: Long): YogaClassResponse {
        logger.debug("Getting class with id: {} for userId: {}", classId, userId)
        
        val yogaClass = yogaClassRepository.findById(classId).orElseThrow {
            IllegalArgumentException("Yoga class not found with id: $classId")
        }
        
        // Validate video URL
        validateVideoUrl(yogaClass.videoUrl)
        
        return toYogaClassResponse(yogaClass)
    }
    
    /**
     * Search classes by text query
     */
    fun searchClasses(searchRequest: ClassSearchRequest, userId: Long): ClassListResponse {
        logger.debug("Searching classes with query: '{}' for userId: {}", searchRequest.query, userId)
        
        val user = getUserById(userId)
        val pageable = createPageable(searchRequest)
        
        val classesPage = if (!searchRequest.query.isNullOrBlank()) {
            yogaClassRepository.searchByTitleDescriptionOrInstructor(
                searchTerm = searchRequest.query.trim(),
                pageable = pageable
            )
        } else {
            // If no search query, fall back to regular filtering
            return getClasses(searchRequest, userId)
        }
        
        val userFavorites = classFavoriteRepository.findFavoriteClassesByUser(user).map { it.id }.toSet()
        
        val classSummaries = classesPage.content.map { yogaClass ->
            toYogaClassSummary(yogaClass, userFavorites.contains(yogaClass.id))
        }
        
        val pagination = PaginationMetadata(
            currentPage = classesPage.number,
            totalPages = classesPage.totalPages,
            totalElements = classesPage.totalElements,
            pageSize = classesPage.size,
            hasNext = classesPage.hasNext(),
            hasPrevious = classesPage.hasPrevious()
        )
        
        return ClassListResponse(
            classes = classSummaries,
            pagination = pagination
        )
    }
    
    /**
     * Add a class to user's favorites
     */
    fun addToFavorites(classId: Long, userId: Long): FavoriteResponse {
        logger.info("Adding class {} to favorites for userId: {}", classId, userId)
        
        val user = getUserById(userId)
        val yogaClass = yogaClassRepository.findById(classId).orElseThrow {
            IllegalArgumentException("Yoga class not found with id: $classId")
        }
        
        // Check if already favorited
        if (classFavoriteRepository.existsByUserAndYogaClass(user, yogaClass)) {
            return FavoriteResponse(
                success = false,
                message = "Class is already in your favorites",
                isFavorite = true
            )
        }
        
        val favorite = ClassFavorite(
            user = user,
            yogaClass = yogaClass
        )
        
        classFavoriteRepository.save(favorite)
        
        logger.info("Successfully added class {} to favorites for userId: {}", classId, userId)
        
        return FavoriteResponse(
            success = true,
            message = "Class added to favorites successfully",
            isFavorite = true
        )
    }
    
    /**
     * Remove a class from user's favorites
     */
    fun removeFromFavorites(classId: Long, userId: Long): FavoriteResponse {
        logger.info("Removing class {} from favorites for userId: {}", classId, userId)
        
        val user = getUserById(userId)
        val yogaClass = yogaClassRepository.findById(classId).orElseThrow {
            IllegalArgumentException("Yoga class not found with id: $classId")
        }
        
        // Check if it's actually favorited
        if (!classFavoriteRepository.existsByUserAndYogaClass(user, yogaClass)) {
            return FavoriteResponse(
                success = false,
                message = "Class is not in your favorites",
                isFavorite = false
            )
        }
        
        val deletedCount = classFavoriteRepository.deleteByUserAndYogaClass(user, yogaClass)
        
        if (deletedCount > 0) {
            logger.info("Successfully removed class {} from favorites for userId: {}", classId, userId)
            return FavoriteResponse(
                success = true,
                message = "Class removed from favorites successfully",
                isFavorite = false
            )
        } else {
            return FavoriteResponse(
                success = false,
                message = "Failed to remove class from favorites",
                isFavorite = true
            )
        }
    }
    
    /**
     * Get user's favorite classes
     */
    fun getFavoriteClasses(userId: Long, page: Int = 0, size: Int = 20): List<FavoriteClassResponse> {
        logger.debug("Getting favorite classes for userId: {}", userId)
        
        val user = getUserById(userId)
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        
        val favoritesPage = classFavoriteRepository.findByUserOrderByCreatedAtDesc(user, pageable)
        
        return favoritesPage.content.map { favorite ->
            FavoriteClassResponse(
                id = favorite.yogaClass.id ?: 0,
                title = favorite.yogaClass.title,
                description = favorite.yogaClass.description,
                durationMinutes = favorite.yogaClass.durationMinutes,
                difficultyLevel = favorite.yogaClass.difficultyLevel.name,
                instructor = favorite.yogaClass.instructor,
                thumbnailUrl = favorite.yogaClass.thumbnailUrl,
                favoritedAt = favorite.createdAt ?: LocalDateTime.now()
            )
        }
    }
    
    /**
     * Check if a class is favorited by user
     */
    fun isClassFavorited(classId: Long, userId: Long): Boolean {
        val user = getUserById(userId)
        val yogaClass = yogaClassRepository.findById(classId).orElseThrow {
            IllegalArgumentException("Yoga class not found with id: $classId")
        }
        
        return classFavoriteRepository.existsByUserAndYogaClass(user, yogaClass)
    }
    
    /**
     * Get user's favorite classes count
     */
    fun getFavoriteClassesCount(userId: Long): Long {
        val user = getUserById(userId)
        return classFavoriteRepository.countByUser(user)
    }
    
    // Private helper methods
    
    private fun getUserById(userId: Long): User {
        return userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found with id: $userId")
        }
    }
    
    private fun createPageable(searchRequest: ClassSearchRequest): Pageable {
        val sortDirection = if (searchRequest.sortDirection.lowercase() == "desc") {
            Sort.Direction.DESC
        } else {
            Sort.Direction.ASC
        }
        
        val sortBy = when (searchRequest.sortBy.lowercase()) {
            "title" -> "title"
            "duration" -> "durationMinutes"
            "difficulty" -> "difficultyLevel"
            "instructor" -> "instructor"
            "created" -> "createdAt"
            else -> "title"
        }
        
        return PageRequest.of(
            searchRequest.page,
            searchRequest.size,
            Sort.by(sortDirection, sortBy)
        )
    }
    
    private fun hasFilters(searchRequest: ClassSearchRequest): Boolean {
        return searchRequest.difficultyLevel != null ||
                searchRequest.minDuration != null ||
                searchRequest.maxDuration != null ||
                !searchRequest.instructor.isNullOrBlank()
    }
    
    private fun validateVideoUrl(videoUrl: String) {
        try {
            val url = URL(videoUrl)
            if (url.protocol != "http" && url.protocol != "https") {
                throw IllegalArgumentException("Video URL must use HTTP or HTTPS protocol")
            }
        } catch (e: Exception) {
            logger.warn("Invalid video URL: {}", videoUrl)
            throw IllegalArgumentException("Invalid video URL format: ${e.message}")
        }
    }
    
    private fun toYogaClassResponse(yogaClass: YogaClass): YogaClassResponse {
        return YogaClassResponse(
            id = yogaClass.id ?: 0,
            title = yogaClass.title,
            description = yogaClass.description,
            durationMinutes = yogaClass.durationMinutes,
            difficultyLevel = yogaClass.difficultyLevel,
            instructor = yogaClass.instructor,
            videoUrl = yogaClass.videoUrl,
            thumbnailUrl = yogaClass.thumbnailUrl,
            createdAt = yogaClass.createdAt,
            updatedAt = yogaClass.updatedAt
        )
    }
    
    private fun toYogaClassSummary(yogaClass: YogaClass, isFavorite: Boolean): YogaClassSummary {
        return YogaClassSummary(
            id = yogaClass.id ?: 0,
            title = yogaClass.title,
            description = yogaClass.description,
            durationMinutes = yogaClass.durationMinutes,
            difficultyLevel = yogaClass.difficultyLevel.name,
            instructor = yogaClass.instructor,
            thumbnailUrl = yogaClass.thumbnailUrl,
            isFavorite = isFavorite
        )
    }
}