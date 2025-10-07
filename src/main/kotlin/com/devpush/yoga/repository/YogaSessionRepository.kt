package com.devpush.yoga.repository

import com.devpush.yoga.entity.User
import com.devpush.yoga.entity.YogaSession
import jakarta.persistence.QueryHint
import java.time.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface YogaSessionRepository : JpaRepository<YogaSession, Long> {

    // Optimized query with pagination support for large datasets
    fun findByUserOrderByCompletedAtDesc(user: User, pageable: Pageable): Page<YogaSession>

    // Keep original method for backward compatibility
    fun findByUserOrderByCompletedAtDesc(user: User): List<YogaSession>

    // Optimized date range query with index hint
    @Query(
            """
        SELECT s FROM YogaSession s 
        WHERE s.user = :user 
        AND s.completedAt BETWEEN :startDate AND :endDate
        ORDER BY s.completedAt DESC
    """
    )
    @QueryHints(
            QueryHint(name = "org.hibernate.cacheable", value = "true"),
            QueryHint(name = "org.hibernate.cacheMode", value = "NORMAL")
    )
    fun findByUserAndCompletedAtBetween(
            @Param("user") user: User,
            @Param("startDate") startDate: LocalDateTime,
            @Param("endDate") endDate: LocalDateTime
    ): List<YogaSession>

    // Optimized count query using index
    @Query("SELECT COUNT(s) FROM YogaSession s WHERE s.user = :user")
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun countByUser(@Param("user") user: User): Long

    // Optimized aggregation query
    @Query("SELECT COALESCE(SUM(s.durationMinutes), 0) FROM YogaSession s WHERE s.user = :user")
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun sumDurationByUser(@Param("user") user: User): Long

    // Optimized calorie sum with null handling
    @Query(
            "SELECT COALESCE(SUM(s.caloriesBurned), 0) FROM YogaSession s WHERE s.user = :user AND s.caloriesBurned IS NOT NULL"
    )
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun sumCaloriesByUser(@Param("user") user: User): Long

    // Optimized date range aggregation using composite index
    @Query(
            """
        SELECT COALESCE(SUM(s.durationMinutes), 0) 
        FROM YogaSession s 
        WHERE s.user = :user 
        AND s.completedAt BETWEEN :startDate AND :endDate
    """
    )
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun sumDurationByUserAndDateRange(
            @Param("user") user: User,
            @Param("startDate") startDate: LocalDateTime,
            @Param("endDate") endDate: LocalDateTime
    ): Long

    // Optimized calorie aggregation for date range
    @Query(
            """
        SELECT COALESCE(SUM(s.caloriesBurned), 0) 
        FROM YogaSession s 
        WHERE s.user = :user 
        AND s.caloriesBurned IS NOT NULL
        AND s.completedAt BETWEEN :startDate AND :endDate
    """
    )
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun sumCaloriesByUserAndDateRange(
            @Param("user") user: User,
            @Param("startDate") startDate: LocalDateTime,
            @Param("endDate") endDate: LocalDateTime
    ): Long

    // ============================================================================
    // ADVANCED ANALYTICS QUERIES WITH DATABASE FUNCTIONS
    // ============================================================================

    // Weekly progress aggregation using database date functions
    @Query(
            """
        SELECT 
            DATE_TRUNC('week', s.completedAt) as week,
            COUNT(s) as sessionCount,
            COALESCE(SUM(s.durationMinutes), 0) as totalDuration,
            COALESCE(SUM(s.caloriesBurned), 0) as totalCalories
        FROM YogaSession s 
        WHERE s.user = :user 
        AND s.completedAt >= :startDate
        GROUP BY DATE_TRUNC('week', s.completedAt)
        ORDER BY week DESC
    """
    )
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun getWeeklyProgressStats(
            @Param("user") user: User,
            @Param("startDate") startDate: LocalDateTime
    ): List<Array<Any>>

    // Monthly progress aggregation using database date functions
    @Query(
            """
        SELECT 
            DATE_TRUNC('month', s.completedAt) as month,
            COUNT(s) as sessionCount,
            COALESCE(SUM(s.durationMinutes), 0) as totalDuration,
            COALESCE(SUM(s.caloriesBurned), 0) as totalCalories
        FROM YogaSession s 
        WHERE s.user = :user 
        AND s.completedAt >= :startDate
        GROUP BY DATE_TRUNC('month', s.completedAt)
        ORDER BY month DESC
    """
    )
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun getMonthlyProgressStats(
            @Param("user") user: User,
            @Param("startDate") startDate: LocalDateTime
    ): List<Array<Any>>

    // Daily session count for streak calculation
    @Query(
            """
        SELECT DATE(s.completedAt) as sessionDate, COUNT(s) as sessionCount
        FROM YogaSession s 
        WHERE s.user = :user 
        AND s.completedAt >= :startDate
        GROUP BY DATE(s.completedAt)
        ORDER BY sessionDate DESC
    """
    )
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun getDailySessionCounts(
            @Param("user") user: User,
            @Param("startDate") startDate: LocalDateTime
    ): List<Array<Any>>

    // Most popular classes for user (for recommendations)
    @Query(
            """
        SELECT s.yogaClass, COUNT(s) as sessionCount
        FROM YogaSession s 
        WHERE s.user = :user 
        AND s.yogaClass IS NOT NULL
        GROUP BY s.yogaClass
        ORDER BY sessionCount DESC
    """
    )
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun getMostPracticedClasses(@Param("user") user: User, pageable: Pageable): Page<Array<Any>>

    // Average session duration by difficulty level
    @Query(
            """
        SELECT yc.difficultyLevel, AVG(s.durationMinutes) as avgDuration
        FROM YogaSession s 
        JOIN s.yogaClass yc
        WHERE s.user = :user 
        AND s.yogaClass IS NOT NULL
        GROUP BY yc.difficultyLevel
    """
    )
    @QueryHints(QueryHint(name = "org.hibernate.cacheable", value = "true"))
    fun getAverageSessionDurationByDifficulty(@Param("user") user: User): List<Array<Any>>

    // Recent sessions with class details (optimized with fetch join)
    @Query(
            """
        SELECT s FROM YogaSession s 
        LEFT JOIN FETCH s.yogaClass yc
        WHERE s.user = :user 
        ORDER BY s.completedAt DESC
    """
    )
    @QueryHints(
            QueryHint(name = "org.hibernate.cacheable", value = "true"),
            QueryHint(name = "org.hibernate.fetchSize", value = "20")
    )
    fun findRecentSessionsWithClassDetails(
            @Param("user") user: User,
            pageable: Pageable
    ): Page<YogaSession>
}
