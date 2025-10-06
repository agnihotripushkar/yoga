package com.devpush.yoga.entity

import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "class_favorites",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_class_favorites_user_class",
            columnNames = ["user_id", "class_id"]
        )
    ],
    indexes = [
        Index(name = "idx_class_favorites_user_id", columnList = "user_id"),
        Index(name = "idx_class_favorites_class_id", columnList = "class_id")
    ]
)
data class ClassFavorite(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    val user: User,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    @NotNull
    val yogaClass: YogaClass,
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null
) {
    // No-arg constructor for JPA
    constructor() : this(
        user = User(),
        yogaClass = YogaClass()
    )
}