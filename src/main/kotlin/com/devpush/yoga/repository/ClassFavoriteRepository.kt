package com.devpush.yoga.repository

import com.devpush.yoga.entity.ClassFavorite
import com.devpush.yoga.entity.User
import com.devpush.yoga.entity.YogaClass
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*
import jakarta.persistence.QueryHint

@Repository
interface ClassFavoriteRepository : JpaRepository<ClassFavorite, Long> {
    
    // Optimized lookup using composite index
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun findByUserAndYogaClass(user: User, yogaClass: YogaClass): Optional<ClassFavorite>
    
    // Optimized user favorites query
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun findByUserOrderByCreatedAtDesc(user: User): List<ClassFavorite>
    
    // Paginated user favorites
    @QueryHints(
        QueryHint(name = "org.hibernate.cacheable", value = "true"),
        QueryHint(name = "org.hibernate.fetchSize", value = "20")
    )
    fun findByUserOrderByCreatedAtDesc(user: User, pageable: Pageable): Page<ClassFavorite>
    
    // Optimized existence check using index
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun existsByUserAndYogaClass(user: User, yogaClass: YogaClass): Boolean
    
    // Optimized deletion with transaction
    @Modifying
    @Transactional
    @Query("DELETE FROM ClassFavorite cf WHERE cf.user = :user AND cf.yogaClass = :yogaClass")
    fun deleteByUserAndYogaClass(@Param("user") user: User, @Param("yogaClass") yogaClass: YogaClass): Int
    
    // Optimized count query
    @Query("SELECT COUNT(cf) FROM ClassFavorite cf WHERE cf.user = :user")
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun countByUser(@Param("user") user: User): Long
    
    // Optimized favorite classes query with fetch join
    @Query("""
        SELECT cf.yogaClass FROM ClassFavorite cf 
        WHERE cf.user = :user 
        ORDER BY cf.createdAt DESC
    """)
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun findFavoriteClassesByUser(@Param("user") user: User): List<YogaClass>
    
    // Paginated favorite classes with fetch optimization
    @Query("""
        SELECT cf.yogaClass FROM ClassFavorite cf 
        WHERE cf.user = :user 
        ORDER BY cf.createdAt DESC
    """)
    @QueryHints(
        QueryHint(name = "org.hibernate.cacheable", value = "true"),
        QueryHint(name = "org.hibernate.fetchSize", value = "20")
    )
    fun findFavoriteClassesByUser(@Param("user") user: User, pageable: Pageable): Page<YogaClass>
    
    // ============================================================================
    // ADVANCED ANALYTICS AND OPTIMIZATION QUERIES
    // ============================================================================
    
    // Favorite classes with full details (optimized with fetch join)
    @Query("""
        SELECT cf FROM ClassFavorite cf 
        JOIN FETCH cf.yogaClass yc
        WHERE cf.user = :user 
        ORDER BY cf.createdAt DESC
    """)
    @QueryHints(
        QueryHint(name = "org.hibernate.cacheable", value = "true"),
        QueryHint(name = "org.hibernate.fetchSize", value = "20")
    )
    fun findFavoriteClassesWithDetails(@Param("user") user: User, pageable: Pageable): Page<ClassFavorite>
    
    // Most favorited classes across all users
    @Query("""
        SELECT cf.yogaClass, COUNT(cf) as favoriteCount
        FROM ClassFavorite cf 
        GROUP BY cf.yogaClass
        ORDER BY favoriteCount DESC, cf.yogaClass.title
    """)
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun findMostFavoritedClasses(pageable: Pageable): Page<Array<Any>>
    
    // User's favorite classes by difficulty level
    @Query("""
        SELECT yc.difficultyLevel, COUNT(cf) as favoriteCount
        FROM ClassFavorite cf 
        JOIN cf.yogaClass yc
        WHERE cf.user = :user
        GROUP BY yc.difficultyLevel
        ORDER BY favoriteCount DESC
    """)
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun getFavoriteCountsByDifficulty(@Param("user") user: User): List<Array<Any>>
    
    // User's favorite instructors
    @Query("""
        SELECT yc.instructor, COUNT(cf) as favoriteCount
        FROM ClassFavorite cf 
        JOIN cf.yogaClass yc
        WHERE cf.user = :user
        AND yc.instructor IS NOT NULL
        GROUP BY yc.instructor
        ORDER BY favoriteCount DESC, yc.instructor
    """)
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun getFavoriteInstructors(@Param("user") user: User): List<Array<Any>>
    
    // Recent favorites activity
    @Query("""
        SELECT cf FROM ClassFavorite cf 
        JOIN FETCH cf.yogaClass yc
        WHERE cf.createdAt >= :sinceDate
        ORDER BY cf.createdAt DESC
    """)
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun findRecentFavorites(@Param("sinceDate") sinceDate: java.time.LocalDateTime, pageable: Pageable): Page<ClassFavorite>
    
    // Classes favorited by users with similar preferences
    @Query("""
        SELECT DISTINCT cf2.yogaClass
        FROM ClassFavorite cf1
        JOIN ClassFavorite cf2 ON cf1.yogaClass = cf2.yogaClass
        WHERE cf1.user = :user
        AND cf2.user != :user
        AND cf2.yogaClass NOT IN (
            SELECT cf3.yogaClass FROM ClassFavorite cf3 WHERE cf3.user = :user
        )
        ORDER BY cf2.yogaClass.title
    """)
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun findRecommendedClassesBasedOnSimilarUsers(@Param("user") user: User, pageable: Pageable): Page<YogaClass>
    
    // Bulk operations for performance
    @Modifying
    @Transactional
    @Query("DELETE FROM ClassFavorite cf WHERE cf.user = :user")
    fun deleteAllByUser(@Param("user") user: User): Int
    
    @Modifying
    @Transactional
    @Query("DELETE FROM ClassFavorite cf WHERE cf.yogaClass = :yogaClass")
    fun deleteAllByYogaClass(@Param("yogaClass") yogaClass: YogaClass): Int
    
    // Batch check for multiple classes
    @Query("""
        SELECT cf.yogaClass.id FROM ClassFavorite cf 
        WHERE cf.user = :user 
        AND cf.yogaClass.id IN :classIds
    """)
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun findFavoritedClassIds(@Param("user") user: User, @Param("classIds") classIds: List<UUID>): List<UUID>
}