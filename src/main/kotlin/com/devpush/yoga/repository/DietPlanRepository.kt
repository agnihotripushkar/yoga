package com.devpush.yoga.repository

import com.devpush.yoga.entity.DietPlan
import com.devpush.yoga.entity.PlanStatus
import com.devpush.yoga.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface DietPlanRepository : JpaRepository<DietPlan, UUID> {
    
    fun findByUserAndStatus(user: User, status: PlanStatus): Optional<DietPlan>
    
    fun findByUserOrderByCreatedAtDesc(user: User): List<DietPlan>
    
    // Find absolute latest plan regardless of status
    fun findFirstByUserOrderByCreatedAtDesc(user: User): Optional<DietPlan>
}
