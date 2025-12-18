package com.devpush.yoga.entity

enum class DietGoal {
    WEIGHT_LOSS,
    WEIGHT_GAIN,
    MAINTENANCE,
    MUSCLE_BUILD,
    IMPROVE_ENERGY
}

enum class DietaryPreference {
    VEGETARIAN,
    NON_VEGETARIAN,
    VEGAN,
    EGGETARIAN,
    KETO,
    PALEO
}

enum class MealType {
    BREAKFAST,
    LUNCH,
    SNACK,
    DINNER,
    PRE_WORKOUT,
    POST_WORKOUT
}

enum class PlanStatus {
    GENERATING,
    ACTIVE,
    COMPLETED,
    ARCHIVED,
    FAILED
}
