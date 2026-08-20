package com.devpush.yoga.repository

import com.devpush.yoga.entity.HealthRecord
import com.devpush.yoga.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

@Repository
interface HealthRecordRepository : JpaRepository<HealthRecord, UUID> {
    
    fun findByUserAndDate(user: User, date: LocalDate): Optional<HealthRecord>
    
    fun findByUserAndDateBetweenOrderByDateAsc(
        user: User, 
        startDate: LocalDate, 
        endDate: LocalDate
    ): List<HealthRecord>
    
    // Find latest record to get current weight etc.
    fun findFirstByUserOrderByDateDesc(user: User): Optional<HealthRecord>
}
