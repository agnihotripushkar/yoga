package com.devpush.yoga.features.diet.dto

import com.devpush.yoga.entity.DietGoal
import com.devpush.yoga.entity.DietaryPreference
import com.devpush.yoga.entity.MealType
import com.devpush.yoga.entity.PlanStatus
import java.time.LocalDateTime
import java.util.UUID

data class GenerateDietPlanRequest(
    val goal: DietGoal,
    val dietaryPreference: DietaryPreference,
    val allergies: String? = null,
    val excludeFoods: String? = null,
    val durationDays: Int = 7 // Default 1 week
)

data class DietPlanResponse(
    val id: UUID,
    val goal: DietGoal,
    val dietaryPreference: DietaryPreference,
    val dailyCaloriesTarget: Int,
    val status: PlanStatus,
    val days: List<DietDayResponse>,
    val createdAt: LocalDateTime
)

data class DietDayResponse(
    val dayNumber: Int,
    val totalCalories: Int,
    val totalProtein: Float,
    val totalCarbs: Float,
    val totalFats: Float,
    val meals: List<MealResponse>
)

data class MealResponse(
    val mealType: MealType,
    val name: String,
    val description: String?,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fats: Float,
    val recipeInstructions: String?
)
