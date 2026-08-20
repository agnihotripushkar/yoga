package com.devpush.yoga.features.wellness.service

import com.devpush.yoga.features.wellness.dto.HealthRecordRequest
import com.devpush.yoga.features.wellness.dto.HealthRecordResponse
import com.devpush.yoga.entity.HealthRecord
import com.devpush.yoga.repository.HealthRecordRepository
import com.devpush.yoga.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional
class WellnessService(
    private val healthRecordRepository: HealthRecordRepository,
    private val userRepository: UserRepository
) {
    
    private val logger = LoggerFactory.getLogger(WellnessService::class.java)
    
    /**
     * Create or update daily health record
     */
    fun logHealthData(userId: UUID, request: HealthRecordRequest): HealthRecordResponse {
        logger.info("Logging health data for userId: {}", userId)
        
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found with id: $userId")
        }
        
        val date = request.date ?: LocalDate.now()
        
        // Check if record already exists for this date
        val existingRecord = healthRecordRepository.findByUserAndDate(user, date)
        
        val record = if (existingRecord.isPresent) {
            val rec = existingRecord.get()
            // Update fields if provided
            request.weight?.let { rec.weight = it }
            request.heartRate?.let { rec.heartRate = it }
            request.hydration?.let { rec.hydration = it }
            request.sleepHours?.let { rec.sleepHours = it }
            request.moodScore?.let { rec.moodScore = it }
            request.stressLevel?.let { rec.stressLevel = it }
            request.notes?.let { rec.notes = it }
            rec
        } else {
            HealthRecord(
                user = user,
                date = date,
                weight = request.weight,
                heartRate = request.heartRate,
                hydration = request.hydration,
                sleepHours = request.sleepHours,
                moodScore = request.moodScore,
                stressLevel = request.stressLevel,
                notes = request.notes
            )
        }
        
        val savedRecord = healthRecordRepository.save(record)
        
        // Update user's current weight if record is for today or future
        if (!date.isBefore(LocalDate.now()) && request.weight != null) {
            user.weight = request.weight
            userRepository.save(user)
        }
        
        logger.info("Successfully logged health data for userId: {} on date: {}", userId, date)
        return toHealthRecordResponse(savedRecord)
    }
    
    /**
     * Get health records for a specific period
     */
    fun getHealthRecords(userId: UUID, startDate: LocalDate, endDate: LocalDate): List<HealthRecordResponse> {
        logger.debug("Getting health records for userId: {} between {} and {}", userId, startDate, endDate)
        
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found with id: $userId")
        }
        
        return healthRecordRepository.findByUserAndDateBetweenOrderByDateAsc(user, startDate, endDate)
            .map { toHealthRecordResponse(it) }
    }
    
    /**
     * Get latest health record
     */
    fun getLatestHealthRecord(userId: UUID): HealthRecordResponse? {
        logger.debug("Getting latest health record for userId: {}", userId)
        
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found with id: $userId")
        }
        
        return healthRecordRepository.findFirstByUserOrderByDateDesc(user)
            .map { toHealthRecordResponse(it) }
            .orElse(null)
    }
    
    private fun toHealthRecordResponse(record: HealthRecord): HealthRecordResponse {
        return HealthRecordResponse(
            id = record.id ?: throw IllegalStateException("ID cannot be null"),
            date = record.date,
            weight = record.weight,
            heartRate = record.heartRate,
            hydration = record.hydration,
            sleepHours = record.sleepHours,
            moodScore = record.moodScore,
            stressLevel = record.stressLevel,
            notes = record.notes,
            createdAt = record.createdAt ?: java.time.LocalDateTime.now(),
            updatedAt = record.updatedAt ?: java.time.LocalDateTime.now()
        )
    }
}
