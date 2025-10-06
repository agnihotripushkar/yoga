package com.devpush.yoga.repository

import com.devpush.yoga.entity.User
import com.devpush.yoga.entity.YogaSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface YogaSessionRepository : JpaRepository<YogaSession, Long> {
    
    fun findByUserOrderByCompletedAtDesc(user: User): List<YogaSession>
    
    fun findByUserAndCompletedAtBetween(
        user: User, 
        startDate: LocalDateTime, 
        endDate: LocalDateTime
    ): List<YogaSession>
    
    @Query("SELECT COUNT(s) FROM YogaSession s WHERE s.user = :user")
    fun countByUser(@Param("user") user: User): Long
    
    @Query("SELECT COALESCE(SUM(s.durationMinutes), 0) FROM YogaSession s WHERE s.user = :user")
    fun sumDurationByUser(@Param("user") user: User): Long
    
    @Query("SELECT COALESCE(SUM(s.caloriesBurned), 0) FROM YogaSession s WHERE s.user = :user AND s.caloriesBurned IS NOT NULL")
    fun sumCaloriesByUser(@Param("user") user: User): Long
    
    @Query("""
        SELECT COALESCE(SUM(s.durationMinutes), 0) 
        FROM YogaSession s 
        WHERE s.user = :user 
        AND s.completedAt BETWEEN :startDate AND :endDate
    """)
    fun sumDurationByUserAndDateRange(
        @Param("user") user: User,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): Long
    
    @Query("""
        SELECT COALESCE(SUM(s.caloriesBurned), 0) 
        FROM YogaSession s 
        WHERE s.user = :user 
        AND s.caloriesBurned IS NOT NULL
        AND s.completedAt BETWEEN :startDate AND :endDate
    """)
    fun sumCaloriesByUserAndDateRange(
        @Param("user") user: User,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): Long
}