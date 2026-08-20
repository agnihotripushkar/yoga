package com.devpush.yoga.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "diet_plans")
data class DietPlan(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val goal: DietGoal,
    
    @Column(name = "dietary_preference", nullable = false)
    @Enumerated(EnumType.STRING)
    val dietaryPreference: DietaryPreference,
    
    @Column(name = "daily_calories_target")
    val dailyCaloriesTarget: Int,
    
    @Column(length = 500)
    val allergies: String? = null,
    
    @Column(name = "exclude_foods", length = 500)
    val excludeFoods: String? = null,
    
    @Enumerated(EnumType.STRING)
    var status: PlanStatus = PlanStatus.GENERATING,
    
    @OneToMany(mappedBy = "dietPlan", cascade = [CascadeType.ALL], orphanRemoval = true)
    val days: MutableList<DietDay> = mutableListOf(),
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null
) {
    constructor() : this(
        user = User(),
        goal = DietGoal.MAINTENANCE,
        dietaryPreference = DietaryPreference.VEGETARIAN,
        dailyCaloriesTarget = 2000
    )
}
