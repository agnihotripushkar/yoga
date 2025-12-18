package com.devpush.yoga.entity

import jakarta.persistence.*
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "health_records",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "date"])
    ],
    indexes = [
        Index(name = "idx_health_records_user_date", columnList = "user_id, date")
    ]
)
data class HealthRecord(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    
    @Column(nullable = false)
    val date: LocalDate,
    
    @Column(name = "weight_kg")
    var weight: Float? = null,
    
    @Column(name = "heart_rate_bpm")
    @Min(0)
    var heartRate: Int? = null,
    
    @Column(name = "hydration_ml")
    @Min(0)
    var hydration: Int? = null,
    
    @Column(name = "sleep_hours")
    @Min(0)
    @Max(24)
    var sleepHours: Float? = null,
    
    @Column(name = "mood_score")
    @Min(1)
    @Max(10)
    var moodScore: Int? = null,
    
    @Column(name = "stress_level")
    @Min(1)
    @Max(10)
    var stressLevel: Int? = null,
    
    @Column(length = 500)
    var notes: String? = null,
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null
) {
    // No-arg constructor for JPA
    constructor() : this(
        user = User(),
        date = LocalDate.now()
    )
}
