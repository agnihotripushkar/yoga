package com.devpush.yoga.features.diet.service


import com.devpush.yoga.entity.*
import com.devpush.yoga.features.diet.dto.*
import com.devpush.yoga.repository.DietPlanRepository
import com.devpush.yoga.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class DietService(
    private val dietPlanRepository: DietPlanRepository,
    private val userRepository: UserRepository,
    // private val geminiClient: GoogleGeminiClient // Assuming this exists or will be created
) {
    
    private val logger = LoggerFactory.getLogger(DietService::class.java)
    
    fun generateDietPlan(userId: UUID, request: GenerateDietPlanRequest): DietPlanResponse {
        logger.info("Generating diet plan for userId: {}", userId)
        
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found with id: $userId")
        }
        
        // Deactivate any existing active plans
        dietPlanRepository.findByUserAndStatus(user, PlanStatus.ACTIVE).ifPresent {
            it.status = PlanStatus.ARCHIVED
            dietPlanRepository.save(it)
        }
        
        // Create new plan structure
        val plan = DietPlan(
            user = user,
            goal = request.goal,
            dietaryPreference = request.dietaryPreference,
            dailyCaloriesTarget = calculateCaloriesTarget(user, request.goal),
            allergies = request.allergies,
            excludeFoods = request.excludeFoods,
            status = PlanStatus.ACTIVE // Ideally GENERATING then updated by async process
        )
        
        // TODO: Call Gemini API here to get meal details
        // For now, generating mock data
        generateMockPlanData(plan, request.durationDays)
        
        val savedPlan = dietPlanRepository.save(plan)
        
        logger.info("Successfully generated diet plan with id: {}", savedPlan.id)
        return toDietPlanResponse(savedPlan)
    }
    
    fun getActiveDietPlan(userId: UUID): DietPlanResponse? {
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found with id: $userId")
        }
        
        return dietPlanRepository.findByUserAndStatus(user, PlanStatus.ACTIVE)
            .map { toDietPlanResponse(it) }
            .orElse(null)
    }
    
    private fun calculateCaloriesTarget(user: User, goal: DietGoal): Int {
        // Simple BMR calculation (Mifflin-St Jeor)
        // If weight/height missing, use defaults
        val weight = user.weight ?: 70f
        val height = user.height ?: 170f
        val age = 30 // Default age if not stored
        val isMale = user.sex == "MALE"
        
        val bmr = if (isMale) {
            (10 * weight) + (6.25 * height) - (5 * age) + 5
        } else {
            (10 * weight) + (6.25 * height) - (5 * age) - 161
        }
        
        val activityMultiplier = when (user.level.toString()) {
            "BEGINNER" -> 1.2
            "INTERMEDIATE" -> 1.375
            "ADVANCED" -> 1.55
            "EXPERT" -> 1.725
            else -> 1.2
        }
        
        val tdee = bmr * activityMultiplier
        
        return when (goal) {
            DietGoal.WEIGHT_LOSS -> (tdee - 500).toInt()
            DietGoal.WEIGHT_GAIN -> (tdee + 500).toInt()
            else -> tdee.toInt()
        }
    }
    
    private fun generateMockPlanData(plan: DietPlan, days: Int) {
        // Mock data generation
        for (i in 1..days) {
            val day = DietDay(
                dietPlan = plan,
                dayNumber = i,
                totalCalories = plan.dailyCaloriesTarget
            )
            
            // Add meals
            val breakfast = Meal(
                dietDay = day,
                mealType = MealType.BREAKFAST,
                name = "Oatmeal with Fruits",
                calories = 400,
                protein = 15f,
                carbs = 60f,
                fats = 8f,
                description = "Healthy oatmeal topped with berries and nuts"
            )
            
            val lunch = Meal(
                dietDay = day,
                mealType = MealType.LUNCH,
                name = "Grilled Chicken Salad",
                calories = 600,
                protein = 40f,
                carbs = 20f,
                fats = 25f,
                description = "Mixed greens with grilled chicken breast"
            )
            
            val dinner = Meal(
                dietDay = day,
                mealType = MealType.DINNER,
                name = "Salmon with Quinoa",
                calories = 700,
                protein = 45f,
                carbs = 50f,
                fats = 30f,
                description = "Baked salmon fillet served with quinoa and asparagus"
            )
            
            day.meals.add(breakfast)
            day.meals.add(lunch)
            day.meals.add(dinner)
            
            plan.days.add(day)
        }
    }
    
    private fun toDietPlanResponse(plan: DietPlan): DietPlanResponse {
        return DietPlanResponse(
            id = plan.id ?: throw IllegalStateException("Plan ID cannot be null"),
            goal = plan.goal,
            dietaryPreference = plan.dietaryPreference,
            dailyCaloriesTarget = plan.dailyCaloriesTarget,
            status = plan.status,
            days = plan.days.sortedBy { it.dayNumber }.map { toDietDayResponse(it) },
            createdAt = plan.createdAt ?: java.time.LocalDateTime.now()
        )
    }
    
    private fun toDietDayResponse(day: DietDay): DietDayResponse {
        return DietDayResponse(
            dayNumber = day.dayNumber,
            totalCalories = day.meals.sumOf { it.calories },
            totalProtein = day.meals.map { it.protein }.sum(),
            totalCarbs = day.meals.map { it.carbs }.sum(),
            totalFats = day.meals.map { it.fats }.sum(),
            meals = day.meals.map { toMealResponse(it) }
        )
    }
    
    private fun toMealResponse(meal: Meal): MealResponse {
        return MealResponse(
            mealType = meal.mealType,
            name = meal.name,
            description = meal.description,
            calories = meal.calories,
            protein = meal.protein,
            carbs = meal.carbs,
            fats = meal.fats,
            recipeInstructions = meal.recipeInstructions
        )
    }
}
