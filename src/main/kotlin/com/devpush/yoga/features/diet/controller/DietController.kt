package com.devpush.yoga.features.diet.controller

import com.devpush.yoga.features.diet.dto.DietPlanResponse
import com.devpush.yoga.features.diet.dto.GenerateDietPlanRequest
import com.devpush.yoga.features.diet.service.DietService
import com.devpush.yoga.features.auth.service.JwtTokenManager
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/diet")
@Tag(name = "AI Diet Plans", description = "AI-powered personalized diet plan generation")
class DietController(
    private val dietService: DietService,
    private val jwtTokenManager: JwtTokenManager
) {
    
    private val logger = LoggerFactory.getLogger(DietController::class.java)
    
    @Operation(
        summary = "Generate Diet Plan",
        description = "Generate a new personalized diet plan based on goal and preferences",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Diet plan generated successfully",
                content = [Content(schema = Schema(implementation = DietPlanResponse::class))]
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    @PostMapping("/generate")
    fun generateDietPlan(
        @Parameter(description = "JWT access token")
        @RequestHeader("Authorization") authorization: String,
        @Valid @RequestBody request: GenerateDietPlanRequest
    ): ResponseEntity<DietPlanResponse> {
        logger.info("Received diet plan generation request")
        
        val userId = extractAndValidateUserId(authorization)
        val plan = dietService.generateDietPlan(userId, request)
        
        return ResponseEntity.ok(plan)
    }
    
    @Operation(
        summary = "Get Active Diet Plan",
        description = "Retrieve the user's current active diet plan",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Active diet plan retrieved successfully",
                content = [Content(schema = Schema(implementation = DietPlanResponse::class))]
            ),
            ApiResponse(responseCode = "404", description = "No active diet plan found"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    @GetMapping("/active")
    fun getActiveDietPlan(
        @Parameter(description = "JWT access token")
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<DietPlanResponse> {
        logger.debug("Received active diet plan request")
        
        val userId = extractAndValidateUserId(authorization)
        val plan = dietService.getActiveDietPlan(userId)
        
        return if (plan != null) {
            ResponseEntity.ok(plan)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    private fun extractAndValidateUserId(authorization: String): UUID {
        val token = if (authorization.startsWith("Bearer ", ignoreCase = true)) {
            authorization.substring(7)
        } else {
            throw RuntimeException("Invalid authorization header format")
        }
        
        val userId = jwtTokenManager.getUserIdFromToken(token)
            ?: throw RuntimeException("Invalid or expired access token")
        
        if (!jwtTokenManager.isAccessToken(token)) {
            throw RuntimeException("Invalid token type. Access token required")
        }
        
        return userId
    }
}
