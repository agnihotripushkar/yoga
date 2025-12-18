package com.devpush.yoga.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "diet_days",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["diet_plan_id", "day_number"])
    ]
)
data class DietDay(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diet_plan_id", nullable = false)
    val dietPlan: DietPlan,
    
    @Column(name = "day_number", nullable = false)
    val dayNumber: Int,
    
    @Column(name = "total_calories")
    var totalCalories: Int = 0,
    
    @Column(name = "total_protein")
    var totalProtein: Float = 0f,
    
    @Column(name = "total_carbs")
    var totalCarbs: Float = 0f,
    
    @Column(name = "total_fats")
    var totalFats: Float = 0f,
    
    @OneToMany(mappedBy = "dietDay", cascade = [CascadeType.ALL], orphanRemoval = true)
    val meals: MutableList<Meal> = mutableListOf()
) {
    constructor() : this(
        dietPlan = DietPlan(),
        dayNumber = 1
    )
}

@Entity
@Table(name = "meals")
data class Meal(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diet_day_id", nullable = false)
    val dietDay: DietDay,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false)
    val mealType: MealType,
    
    @Column(nullable = false)
    val name: String,
    
    @Column(length = 1000)
    val description: String? = null,
    
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fats: Float,
    
    @Column(name = "recipe_instructions", length = 2000)
    val recipeInstructions: String? = null
) {
    constructor() : this(
        dietDay = DietDay(),
        mealType = MealType.BREAKFAST,
        name = "",
        calories = 0,
        protein = 0f,
        carbs = 0f,
        fats = 0f
    )
}
