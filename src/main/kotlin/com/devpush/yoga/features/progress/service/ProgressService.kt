package com.devpush.yoga.features.progress.service

import com.devpush.yoga.features.progress.dto.*
import com.devpush.yoga.entity.DifficultyLevel
import com.devpush.yoga.entity.User
import com.devpush.yoga.entity.YogaClass
import com.devpush.yoga.entity.YogaSession
import com.devpush.yoga.repository.UserRepository
import com.devpush.yoga.repository.YogaClassRepository
import com.devpush.yoga.repository.YogaSessionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.*
import kotlin.math.roundToInt

@Service
@Transactional
class ProgressService(
    private val yogaSessionRepository: YogaSessionRepository,
    private val userRepository: UserRepository,
    private val yogaClassRepository: YogaClassRepository
) {
    
    private val logger = LoggerFactory.getLogger(ProgressService::class.java)
    
    /**
     * Record a completed yoga session for a user
     */
    fun recordSession(userId: UUID, sessionRequest: SessionRecordRequest): SessionResponse {
        logger.info("Recording session for userId: {}", userId)
        
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found with id: $userId")
        }
        
        val yogaClass = sessionRequest.classId?.let { classId ->
            yogaClassRepository.findById(classId).orElseThrow {
                IllegalArgumentException("Yoga class not found with id: $classId")
            }
        }
        
        // Calculate calories if not provided
        val calories = sessionRequest.caloriesBurned ?: calculateCalories(
            sessionRequest.durationMinutes,
            yogaClass?.difficultyLevel
        )
        
        val session = YogaSession(
            user = user,
            yogaClass = yogaClass,
            durationMinutes = sessionRequest.durationMinutes,
            caloriesBurned = calories,
            classType = sessionRequest.classType,
            completed = sessionRequest.completed,
            notes = sessionRequest.notes
        )
        
        val savedSession = yogaSessionRepository.save(session)
        logger.info("Successfully recorded session with id: {} for userId: {}", savedSession.id, userId)
        
        return toSessionResponse(savedSession)
    }
    
    /**
     * Get overall progress summary for a user
     */
    fun getProgressSummary(userId: UUID): ProgressSummary {
        logger.debug("Getting progress summary for userId: {}", userId)
        
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found with id: $userId")
        }
        
        val sessions = yogaSessionRepository.findByUserOrderByCompletedAtDesc(user)
        
        if (sessions.isEmpty()) {
            return ProgressSummary()
        }
        
        val totalSessions = sessions.size
        val totalDuration = sessions.sumOf { it.durationMinutes }
        val totalCalories = sessions.sumOf { it.caloriesBurned ?: 0 }
        val averageDuration = if (totalSessions > 0) totalDuration.toDouble() / totalSessions else 0.0
        val averageCalories = if (totalSessions > 0) totalCalories.toDouble() / totalSessions else 0.0
        
        val firstSession = sessions.minByOrNull { it.completedAt ?: LocalDateTime.now() }
        val lastSession = sessions.maxByOrNull { it.completedAt ?: LocalDateTime.now() }
        
        val currentStreak = calculateCurrentStreak(sessions)
        val longestStreak = calculateLongestStreak(sessions)
        
        return ProgressSummary(
            totalSessions = totalSessions,
            totalDurationMinutes = totalDuration,
            totalCaloriesBurned = totalCalories,
            averageSessionDuration = averageDuration,
            averageCaloriesPerSession = averageCalories,
            firstSessionDate = firstSession?.completedAt,
            lastSessionDate = lastSession?.completedAt,
            currentStreak = currentStreak,
            longestStreak = longestStreak
        )
    }
    
    /**
     * Get weekly progress data for a user
     */
    fun getWeeklyProgress(userId: UUID, weekOffset: Int = 0): WeeklyProgress {
        logger.debug("Getting weekly progress for userId: {} with offset: {}", userId, weekOffset)
        
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found with id: $userId")
        }
        
        val weekFields = WeekFields.of(Locale.getDefault())
        val today = LocalDate.now()
        val targetWeek = today.minusWeeks(weekOffset.toLong())
        val weekStart = targetWeek.with(weekFields.dayOfWeek(), 1)
        val weekEnd = weekStart.plusDays(6)
        
        val startDateTime = weekStart.atStartOfDay()
        val endDateTime = weekEnd.atTime(23, 59, 59)
        
        val sessions = yogaSessionRepository.findByUserAndCompletedAtBetween(user, startDateTime, endDateTime)
        
        if (sessions.isEmpty()) {
            return WeeklyProgress(
                weekStartDate = weekStart,
                weekEndDate = weekEnd
            )
        }
        
        val totalSessions = sessions.size
        val totalDuration = sessions.sumOf { it.durationMinutes }
        val totalCalories = sessions.sumOf { it.caloriesBurned ?: 0 }
        val averageDuration = if (totalSessions > 0) totalDuration.toDouble() / totalSessions else 0.0
        
        // Group sessions by day
        val sessionsByDay = sessions.groupBy { 
            it.completedAt?.toLocalDate() ?: LocalDate.now()
        }
        
        val daysActive = sessionsByDay.keys.size
        
        // Create daily breakdown
        val dailyBreakdown = (0..6).map { dayOffset ->
            val date = weekStart.plusDays(dayOffset.toLong())
            val daySessions = sessionsByDay[date] ?: emptyList()
            
            DailyProgress(
                date = date,
                sessions = daySessions.size,
                durationMinutes = daySessions.sumOf { it.durationMinutes },
                caloriesBurned = daySessions.sumOf { it.caloriesBurned ?: 0 }
            )
        }
        
        return WeeklyProgress(
            weekStartDate = weekStart,
            weekEndDate = weekEnd,
            sessions = totalSessions,
            totalDurationMinutes = totalDuration,
            totalCaloriesBurned = totalCalories,
            averageSessionDuration = averageDuration,
            daysActive = daysActive,
            dailyBreakdown = dailyBreakdown
        )
    }
    
    /**
     * Get monthly progress data for a user
     */
    fun getMonthlyProgress(userId: UUID, monthOffset: Int = 0): MonthlyProgress {
        logger.debug("Getting monthly progress for userId: {} with offset: {}", userId, monthOffset)
        
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found with id: $userId")
        }
        
        val today = LocalDate.now()
        val targetMonth = today.minusMonths(monthOffset.toLong())
        val monthStart = targetMonth.withDayOfMonth(1)
        val monthEnd = targetMonth.withDayOfMonth(targetMonth.lengthOfMonth())
        
        val startDateTime = monthStart.atStartOfDay()
        val endDateTime = monthEnd.atTime(23, 59, 59)
        
        val sessions = yogaSessionRepository.findByUserAndCompletedAtBetween(user, startDateTime, endDateTime)
        
        if (sessions.isEmpty()) {
            return MonthlyProgress(
                monthStartDate = monthStart,
                monthEndDate = monthEnd
            )
        }
        
        val totalSessions = sessions.size
        val totalDuration = sessions.sumOf { it.durationMinutes }
        val totalCalories = sessions.sumOf { it.caloriesBurned ?: 0 }
        val averageDuration = if (totalSessions > 0) totalDuration.toDouble() / totalSessions else 0.0
        
        // Group sessions by day to count active days
        val sessionsByDay = sessions.groupBy { 
            it.completedAt?.toLocalDate() ?: LocalDate.now()
        }
        val daysActive = sessionsByDay.keys.size
        
        // Create weekly breakdown
        val weekFields = WeekFields.of(Locale.getDefault())
        val weeklyBreakdown = mutableListOf<WeeklyProgressSummary>()
        
        var currentWeekStart = monthStart.with(weekFields.dayOfWeek(), 1)
        if (currentWeekStart.isBefore(monthStart)) {
            currentWeekStart = monthStart
        }
        
        var weekNumber = 1
        while (currentWeekStart.isBefore(monthEnd) || currentWeekStart.isEqual(monthEnd)) {
            val weekEnd = minOf(currentWeekStart.plusDays(6), monthEnd)
            
            val weekSessions = sessions.filter { session ->
                val sessionDate = session.completedAt?.toLocalDate()
                sessionDate != null && 
                !sessionDate.isBefore(currentWeekStart) && 
                !sessionDate.isAfter(weekEnd)
            }
            
            weeklyBreakdown.add(
                WeeklyProgressSummary(
                    weekNumber = weekNumber,
                    weekStartDate = currentWeekStart,
                    weekEndDate = weekEnd,
                    sessions = weekSessions.size,
                    durationMinutes = weekSessions.sumOf { it.durationMinutes },
                    caloriesBurned = weekSessions.sumOf { it.caloriesBurned ?: 0 }
                )
            )
            
            currentWeekStart = currentWeekStart.plusWeeks(1)
            weekNumber++
        }
        
        return MonthlyProgress(
            monthStartDate = monthStart,
            monthEndDate = monthEnd,
            sessions = totalSessions,
            totalDurationMinutes = totalDuration,
            totalCaloriesBurned = totalCalories,
            averageSessionDuration = averageDuration,
            daysActive = daysActive,
            weeklyBreakdown = weeklyBreakdown
        )
    }
    
    /**
     * Calculate estimated calories burned based on duration and difficulty
     */
    private fun calculateCalories(durationMinutes: Int, difficultyLevel: DifficultyLevel?): Int {
        // Base calories per minute for yoga (varies by difficulty)
        val caloriesPerMinute = when (difficultyLevel) {
            DifficultyLevel.BEGINNER -> 3.0      // Gentle yoga
            DifficultyLevel.INTERMEDIATE -> 4.5   // Hatha/Vinyasa yoga
            DifficultyLevel.ADVANCED -> 6.0      // Power/Ashtanga yoga
            DifficultyLevel.EXPERT -> 7.5        // Advanced logic
            null -> 4.0                          // Default moderate intensity
        }
        
        return (durationMinutes * caloriesPerMinute).roundToInt()
    }
    
    /**
     * Calculate current streak of consecutive days with sessions
     */
    private fun calculateCurrentStreak(sessions: List<YogaSession>): Int {
        if (sessions.isEmpty()) return 0
        
        val sessionDates = sessions
            .mapNotNull { it.completedAt?.toLocalDate() }
            .distinct()
            .sortedDescending()
        
        if (sessionDates.isEmpty()) return 0
        
        val today = LocalDate.now()
        
        // Check if there's a session today or yesterday (to account for different time zones)
        return if (sessionDates.first() == today || sessionDates.first() == today.minusDays(1)) {
            var streak = 1
            val startDate = sessionDates.first()
            
            // Count consecutive days backwards
            for (i in 1 until sessionDates.size) {
                val expectedDate = startDate.minusDays(i.toLong())
                if (sessionDates.contains(expectedDate)) {
                    streak++
                } else {
                    break
                }
            }
            
            streak
        } else {
            0
        }
    }
    
    /**
     * Calculate longest streak of consecutive days with sessions
     */
    private fun calculateLongestStreak(sessions: List<YogaSession>): Int {
        if (sessions.isEmpty()) return 0
        
        val sessionDates = sessions
            .mapNotNull { it.completedAt?.toLocalDate() }
            .distinct()
            .sorted()
        
        if (sessionDates.isEmpty()) return 0
        
        var longestStreak = 1
        var currentStreak = 1
        
        for (i in 1 until sessionDates.size) {
            val daysBetween = ChronoUnit.DAYS.between(sessionDates[i - 1], sessionDates[i])
            
            if (daysBetween == 1L) {
                currentStreak++
                longestStreak = maxOf(longestStreak, currentStreak)
            } else {
                currentStreak = 1
            }
        }
        
        return longestStreak
    }
    
    /**
     * Convert YogaSession entity to SessionResponse DTO
     */
    private fun toSessionResponse(session: YogaSession): SessionResponse {
        val yogaClassInfo = session.yogaClass?.let { yogaClass ->
            SessionYogaClassInfo(
                id = yogaClass.id ?: throw IllegalStateException("Class ID cannot be null"),
                title = yogaClass.title,
                instructor = yogaClass.instructor,
                difficultyLevel = yogaClass.difficultyLevel.name
            )
        }
        
        return SessionResponse(
            id = session.id ?: throw IllegalStateException("Session ID cannot be null"),
            durationMinutes = session.durationMinutes,
            caloriesBurned = session.caloriesBurned,
            classType = session.classType,
            completed = session.completed,
            completedAt = session.completedAt ?: LocalDateTime.now(),
            notes = session.notes,
            yogaClass = yogaClassInfo
        )
    }
}