package com.devpush.yoga.features.classes.dto

import java.util.UUID

data class ClassListResponse(
    val classes: List<YogaClassSummary>,
    val pagination: PaginationMetadata
)

data class YogaClassSummary(
    val id: UUID,
    val title: String,
    val description: String?,
    val durationMinutes: Int,
    val difficultyLevel: String,
    val instructor: String?,
    val thumbnailUrl: String?,
    val isFavorite: Boolean = false
)

data class PaginationMetadata(
    val currentPage: Int,
    val totalPages: Int,
    val totalElements: Long,
    val pageSize: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)